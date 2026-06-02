import { onCall, HttpsError } from "firebase-functions/v2/https";
import { SecretParam } from "firebase-functions/params";
import { callGemini } from "../services/gemini";

export const createAiraGeneral = (geminiKey: SecretParam) => onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { prompt, bookTitle, bookAuthor, history } = request.data;

    const systemPrompt = `
        You are Aira, a warm and passionate reading companion who loves books and literature.
        The user is currently reading "${bookTitle}" by ${bookAuthor}.
        Answer naturally and conversationally using your own knowledge.
        You may discuss the book's themes, the author, historical context, or anything relevant.
        Use plain text only. No markdown, no bold, no headers.
        Keep answers to 3-5 sentences. Be engaging, warm, and enthusiastic about books.
        Do not reference any retrieved passages — answer purely from your knowledge.
    `.trim();

    const text = await callGemini(
      geminiKey.value(),
      prompt,
      systemPrompt,
      history ?? []
    );

    return { response: text.split("**").join("").split("*").join("") };
  }
);
