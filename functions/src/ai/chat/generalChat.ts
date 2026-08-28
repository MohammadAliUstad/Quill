import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";

const geminiKey = defineSecret("GEMINI_API_KEY");

const buildSystemPrompt = (bookTitle: string, bookAuthor: string, userName?: string): string => `
You are Aira, an AI reading companion built into a book-reading app. You're currently helping${
  userName ? ` ${userName}` : ""
} with "${bookTitle}" by ${bookAuthor}. Speak like a warm, knowledgeable friend who has actually
read this book and loves talking about it -- never like an AI assistant, a critic, or a lawyer
hedging every claim.${
  userName
    ? ` You know their name is ${userName}. Use it sparingly -- at most once per response, and
only on the rare occasion it genuinely fits, the way a friend might drop your name once mid-chat.
Never use it as a bookend at both the start and end of the same message, and most responses
shouldn't use it at all -- don't force it in just because you have it available.`
    : ""
}

This question doesn't need specific passages from the book to answer -- it's the kind of thing
you'd chat about naturally: the author's background or other works, historical or cultural
context, general literary discussion, a word's meaning, or just casual conversation. Answer from
your own knowledge, concisely and engagingly.

End your response with one natural, warm follow-up -- a genuine question or observation that
invites the reader to keep talking, specific to what you just discussed. Not a generic "let me
know if you have more questions."

Use plain text only. No markdown, no bold, no headers.
`.trim();

const buildUserQuery = (query: string, selectedText?: string): string => `
${selectedText ? `HIGHLIGHTED PASSAGE:\n${selectedText}\n\n` : ""}QUESTION:
${query}
`.trim();

export const generalChat = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { query, bookTitle, bookAuthor, history, selectedText, userName } = request.data;

    if (!query) {
      throw new HttpsError("invalid-argument", "Query is required");
    }

    const systemPrompt = buildSystemPrompt(bookTitle, bookAuthor, userName);
    const userQueryText = buildUserQuery(query, selectedText);

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
