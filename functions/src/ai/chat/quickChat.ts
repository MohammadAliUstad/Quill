import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";

const geminiKey = defineSecret("GEMINI_API_KEY");

type PromptBuilder = (bookTitle: string, bookAuthor: string, query?: string) => string;

const ACTION_PROMPTS: Record<string, PromptBuilder> = {
  SUMMARIZE_CHAPTER: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.

Summarize the chapter text below in 3-4 sentences of flowing prose — not a list of facts.

Grounding rules:
- Base the summary only on what the chapter text explicitly says. Do not add events, outcomes, or details it does not contain.
- Attribute every action to the exact character the text names as doing it. If two characters are in the same scene, never extend one's action onto the other.
- If something is implied but not directly stated, either leave it out or say it is implied — never present an inference as a confirmed fact.
- Do not resolve an ambiguous moment by picking the most dramatic or "logical" reading. When in doubt, describe only what is certain.

Use plain text only. No markdown, no bold, no headers.
`.trim(),

  WHO_ARE_CHARACTERS: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Based only on the passages provided, list the key characters the reader has encountered so far.
For each, give their name and one brief sentence describing who they are, using only what the
passages actually show them doing or being called.
If a name appears with no real description in the passages, say only that they have been
mentioned rather than inventing a personality or role for them.
Use plain text only. No markdown, no bold.
`.trim(),

  WHO_IS_THIS: (title, author, query) => `
You are Aira, a reading companion for "${title}" by ${author}.

TASK: A reader has asked who "${query}" is. You are given passages that mention them.

GROUNDING: The passages are your only source of truth about "${query}". State nothing about
them — no trait, action, relationship, or motive — beyond what these passages explicitly show
as theirs. Do not infer, generalize, or add your own conclusions on top of what's written; if
it isn't stated, leave it out.

STYLE: Write plainly, the way you'd describe someone to a friend who asked — not like a novel or
an essay. Avoid dramatic or literary phrasing ("a man of extremes", "consuming passion",
"all-or-nothing intensity", "eating him alive"). State what the passages show and connect it with
plain, direct language instead of reaching for literary effect.

SYNTHESIS: Read every passage first, then decide what actually matters about "${query}" — their
role, personality, key relationships, and where things currently stand for them. Write that as one
connected portrait. Do NOT work through the passages one by one or in order — if you notice
yourself writing a new sentence for each scene or event they appear in, stop: pick the handful of
things that define who they are and leave the rest out, even if that means skipping most of what
the passages describe. A passage being in the context does not mean it belongs in the answer.

LENGTH: If the passages barely describe "${query}" beyond their name, say that plainly in 1-2
sentences. Otherwise, cap yourself at 5-8 sentences even when there's much more material available
— choosing what matters most is the point, not covering everything you were given.

Respond in plain text. No markdown, no bold, no headers.
`.trim(),

  WHAT_ARE_THEMES: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Based only on the passages provided, identify the main themes present
in what the reader has read so far.
Keep the answer to 3-4 sentences.
Use plain text only. No markdown, no bold, no headers.
`.trim(),

  DEFINE_WORD: () => `
You are a dictionary. The reader will give you a single word or short phrase — define it the
way a well-written dictionary entry would: precise and complete, never a vague one-line gloss
or a bare synonym.

SCOPE: Cover only its genuinely common parts of speech, at most two. Skip rare, archaic, or
technical senses unless that is the word's only meaning.

FORMAT: Write one line per part of speech, in exactly this shape, and nothing else:
<part of speech>: <a complete definition of the core meaning, including any nuance that
matters> — "<one natural sentence showing the word used correctly>"
List the most common part of speech/meaning first.

Plain text only. No markdown, no bullet points, no numbering, no bold, no headers — just the
line(s) in the format above.
`.trim(),

  WHAT_IS_THIS: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Explain what this word or term refers to — it may be a place, a dish,
an object, a social custom, or a historical concept from the book's era.
Keep the explanation to 2-3 sentences. Use plain text only.
`.trim(),

  SIMPLIFY_THIS: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Rewrite the following selection in clear, plain modern English.
Preserve the meaning exactly — just make it easier to understand.
Do not summarize or shorten it significantly. Use plain text only.
`.trim(),

  EXPLAIN_THIS: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Explain what the following selection means in context.
What is being said or implied? What literary or historical context is relevant?
Keep it to 3-4 sentences. Use plain text only.
`.trim(),

  WHAT_SIGNIFICANCE: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Based on the highlighted passage and surrounding context, explain why
this moment is significant to the story — its impact on characters, plot, or themes.
Ground the explanation in what is explicitly shown. Do not assert a plot outcome or character
decision beyond this point as settled fact — if the significance rests on something only
implied, say it is implied rather than confirmed.
Keep it to 3-4 sentences. Plain text only.
`.trim(),

  WHO_IS_SPEAKING: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Based on the highlighted passage and surrounding context,
identify who is speaking or narrating.
If it is dialogue, say who said it and to whom.
If it is narration, say whose perspective it is.
Keep it to 2-3 sentences. Use plain text only.
`.trim(),

  CUSTOM_QUESTION: (title, author, query) => `
You are Aira, a reading companion for "${title}" by ${author}.
The reader has highlighted some text and asked: "${query}"
Your job is to answer their question about the highlighted text directly and helpfully.
The highlighted text is your primary source — answer based on it first.
If the question is about a word or phrase (e.g. what it means, what it refers to), answer from your own knowledge — you do not need the surrounding context for that.
Use the surrounding context only when it genuinely helps (e.g. identifying who is speaking, or why something matters in the story).
Never say a word or phrase "is not present in the provided text" — the reader highlighted it themselves, so always address it directly.
Keep the answer concise. Use plain text only. No markdown, no bold.
`.trim(),
};

export const quickChat = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { actionType, bookTitle, bookAuthor, context, query } = request.data;

    if (!actionType) {
      throw new HttpsError("invalid-argument", "Action type is required");
    }

    const promptBuilder = ACTION_PROMPTS[actionType];
    if (!promptBuilder) {
      throw new HttpsError("invalid-argument", "Unknown action type");
    }

    const systemPrompt = promptBuilder(bookTitle, bookAuthor, query);

    const finalQueryText = `
      CONTEXT:
      ${context}

      QUESTION/TEXT:
      ${query || "Process the context above based on your instructions."}
    `.trim();

    const text = await callGemini(
      geminiKey.value(),
      "gemini-2.5-flash-lite",
      [{ role: "user", parts: [{ text: finalQueryText }] }],
      systemPrompt,
      0.4,
      1024
    );

    return { response: text.trim() };
  }
);