import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";

const geminiKey = defineSecret("GEMINI_API_KEY");

const buildRouterPrompt = (
  bookTitle: string,
  bookAuthor: string,
  query: string,
  selectedText?: string
): string => `
You are a query classifier for a book reading app.
The user is currently reading "${bookTitle}" by ${bookAuthor}.
${
  selectedText
    ? `The reader highlighted this passage from the book before asking their question:\n"""\n${selectedText}\n"""\nResolve any pronoun or vague reference in the question (e.g. "he", "this", "why did they") against who or what this passage is about.\n`
    : ""
}
Classify whether the following question requires retrieving specific passages from the book, or can be answered from general knowledge.

RAG is required when the question asks about:
- Specific events, scenes, or moments in the book
- Character actions, dialogue, or relationships within the story
- Plot details or story progression
- Direct quotes or specific wording from the book
- A summary of the current chapter
- A theme or the significance of something, when grounded in the story itself

RAG is NOT required when the question asks about:
- General information about the book (synopsis, genre)
- The author's background or other works
- Historical or cultural context around the book
- General literary discussion or recommendations
- Casual conversation or greetings
- The meaning of a specific word or phrase (a vocabulary question, not a plot question)

GROUNDING RULE — this is critical: only use a name if you are certain it belongs to a character,
place, or thing in THIS specific book, "${bookTitle}" by ${bookAuthor}. Never invent, guess, or
borrow a name from a different book, even one by the same author. If the question refers to
someone by role or relation (e.g. "Katya's father", "the colonel") and you do not know their
given name with certainty, keep referring to them exactly as the question does — do NOT expand
or fabricate a name. This applies to entities, queryVariations, keywords, and characterName alike.

If RAG is required, classify queryIntent as exactly one of:
- chapter_summary: asking for a summary/recap of the current chapter (not a specific past chapter
  by number -- always treat this as "the chapter I'm currently on")
- character_profile: asking who a character fundamentally is -- personality, role, relationships
- character_recent: asking what's currently happening for a character / how things stand for them
  right now (not their general backstory)
- character_arc: asking how a character has changed, developed, or what their journey has been
  like over the course of the story so far
- relationship: asking about the relationship or dynamic between two or more named people
- plot_event: asking what happened, or why something happened -- especially when it may require
  connecting multiple separate moments in the book (e.g. "why did X do Y a second time"). For a
  "who did X" / "who is responsible for X" style question, remember the true answer in a novel is
  often only revealed later via a confession, discovery, or admission rather than at the moment X
  happened -- make one of the queryVariations specifically target that kind of later reveal (e.g.
  "who confesses to X", "who admits to X", "the truth about X is revealed"), not just variations
  of the question's own surface phrasing
- theme_analysis: asking about a theme, motif, or symbolic meaning
- significance: asking why a specific moment, detail, or choice matters
- quote_lookup: asking about specific wording or a direct quote

Also extract:
- entities: all character names, place names, and proper nouns mentioned in the question. Only
  expand to a full formal name if you are certain it's correct for this book (see GROUNDING RULE
  above) — otherwise use the name or description exactly as it appears in the question.
- keywords: the most meaningful content words from the question, excluding stop words, that would likely appear verbatim in the relevant passage
- characterName: ONLY when queryIntent is character_profile, character_recent, or character_arc --
  the single character the question is about, exactly as named (empty string otherwise, and when
  there isn't a single clear character, e.g. the question is really about two people -- use
  relationship instead in that case)
- isChapterScoped: ONLY meaningful when queryIntent is theme_analysis or significance -- true if
  the question is about the current chapter/what they just read specifically (e.g. "in this
  chapter", "what I just read"), false if it's about the book as a whole. Default false.

Respond ONLY with a valid JSON object. No preamble, no explanation, no markdown.

If RAG is required:
{
  "isRAG": true,
  "queryVariations": [
    "variation one as a declarative phrase",
    "variation two from a different angle",
    "variation three emphasizing a different aspect"
  ],
  "entities": ["Full Name One", "Full Name Two"],
  "keywords": ["keyword1", "keyword2", "keyword3"],
  "queryIntent": "plot_event",
  "characterName": "",
  "isChapterScoped": false
}

If RAG is not required:
{
  "isRAG": false,
  "queryVariations": [],
  "entities": [],
  "keywords": [],
  "queryIntent": "general",
  "characterName": "",
  "isChapterScoped": false
}

Question: ${query}
`.trim();

export const detectIntent = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { query, bookTitle, bookAuthor, selectedText } = request.data;

    if (!query) {
      throw new HttpsError("invalid-argument", "Query is required");
    }

    const routerPrompt = buildRouterPrompt(bookTitle, bookAuthor, query, selectedText);

    const text = await callGemini(
      geminiKey.value(),
      "gemini-2.5-flash-lite",
      [{ role: "user", parts: [{ text: routerPrompt }] }],
      undefined, // No system instruction needed for this specific one
      0.1,
      256
    );

    return { response: text.trim() };
  }
);
