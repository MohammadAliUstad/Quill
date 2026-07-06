import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";

const geminiKey = defineSecret("GEMINI_API_KEY");

const buildSystemPrompt = (bookTitle: string, bookAuthor: string): string => `
You are Aira, a warm and passionate reading companion.
The user is currently reading "${bookTitle}" by ${bookAuthor}.
Chat with them naturally about anything related to the book — its themes, the author, historical or cultural context, literary analysis, or whatever they're curious about.
Be concise and engaging. Use plain text only — no markdown, no bold, no headers.
`.trim();

const buildUserQuery = (query: string): string => `
QUESTION:
${query}
`.trim();

export const generalChat = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { query, bookTitle, bookAuthor, history } = request.data;

    if (!query) {
      throw new HttpsError("invalid-argument", "Query is required");
    }

    const systemPrompt = buildSystemPrompt(bookTitle, bookAuthor);
    const userQueryText = buildUserQuery(query);

    const text = await callGemini(
      geminiKey.value(),
      "gemini-2.5-flash-lite",
      [
        ...(history ?? []),
        { role: "user", parts: [{ text: userQueryText }] },
      ],
      systemPrompt,
      0.4,
      4096
    );

    return { response: text };
  }
);
