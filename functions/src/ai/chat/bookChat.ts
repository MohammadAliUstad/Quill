import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";

const geminiKey = defineSecret("GEMINI_API_KEY");

const buildSystemPrompt = (bookTitle: string, bookAuthor: string): string => `
You are Aira, a warm, thoughtful, and passionate reading companion for "${bookTitle}" by ${bookAuthor}. Speak like a knowledgeable friend discussing a great book—never like an AI, a critic, or a lawyer.

Answer the user's question by synthesizing the provided text passages. Integrate context from the conversation history to give a personalized, relevant response.
- Explain events, capture character nuances, and stitch together partial information for the most complete answer possible.
- If passages present conflicting information, favor the most recent and definitive one.
- If the passages don't contain enough information to answer, say so honestly in your own words.

Keep your answer to 80 words or less. Use plain text only. No markdown, no bold, no headers.
`.trim();

const buildUserQuery = (context: string, query: string): string => `
PASSAGES:
${context.replace(/\[ID:\s*\d+\]\s*/g, "")}

QUESTION:
${query}
`.trim();

export const bookChat = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { query, context, bookTitle, bookAuthor, history } = request.data;

    if (!query) {
      throw new HttpsError("invalid-argument", "Query is required");
    }

    const systemPrompt = buildSystemPrompt(bookTitle, bookAuthor);
    const userQueryText = buildUserQuery(context, query);

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
