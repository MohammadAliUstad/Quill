import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";

const geminiKey = defineSecret("GEMINI_API_KEY");

type PromptBuilder = (bookTitle: string, bookAuthor: string, query?: string) => string;

const ACTION_PROMPTS: Record<string, PromptBuilder> = {
  SUMMARIZE_CHAPTER: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Summarize the following chapter text in 3-4 sentences.
Focus on the key events, character actions, and any important revelations.
Use plain text only. No markdown, no bold, no headers.
`.trim(),

  WHO_ARE_CHARACTERS: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Based only on the passages provided, list the key characters the reader has encountered so far.
For each, give their name and one brief sentence describing who they are.
Use plain text only. No markdown, no bold.
`.trim(),

  WHO_IS_THIS: (title, author, query) => `
You are Aira, a reading companion for "${title}" by ${author}.
Based ONLY on the passages provided, explain who "${query}" is.
Describe their role, personality, or relationship to other characters
as shown in what the reader has read so far.
Do NOT reveal anything beyond what the passages contain.
Keep it to 3-4 sentences. Use plain text only. No markdown, no bold.
`.trim(),

  WHAT_ARE_THEMES: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Based only on the passages provided, identify the main themes present
in what the reader has read so far.
Keep the answer to 3-4 sentences.
Use plain text only. No markdown, no bold, no headers.
`.trim(),

  DEFINE_WORD: () => `
You are a precise dictionary. Give a concise definition of the word provided.
Include the part of speech and one example sentence if helpful.
Keep it to 2-3 sentences maximum. Use plain text only.
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