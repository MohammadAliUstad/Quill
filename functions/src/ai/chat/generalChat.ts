import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";
import { SETTINGS } from "../../core/models";
import { buildChatPrompt, cleanHistory, ReaderContext, readerContextFrom } from "../prompts/aira";

const geminiKey = defineSecret("GEMINI_API_KEY");

// Answered from the model's own knowledge, which includes how the book ends: the spoiler rule in
// the base prompt is what keeps that out.
const task = (ctx: ReaderContext) => `
Talk about it the way a well-read friend would: the author's life and other work, the history and
culture around the book, how it was received, what a word or phrase means, or a passing remark
like a greeting or thanks (answer that in a line). Bring in the detail that makes it interesting
rather than an encyclopedia summary. If it's outside what you're for, decline as described above.${
  ctx.spoilerLock
    ? `
If it asks what happens in the story, you have no passages here: say you'd rather not risk
spoiling anything, and that if they ask about the part they've read you'll look at the book.`
    : ""
}
`.trim();

const buildUserMessage = (query: string, selectedText?: string): string => `
${selectedText ? `HIGHLIGHTED PASSAGE:\n${selectedText}\n\n` : ""}QUESTION:
${query}
`.trim();

export const generalChat = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { query, history, selectedText } = request.data;

    if (!query) {
      throw new HttpsError("invalid-argument", "Query is required");
    }

    const ctx = readerContextFrom(request.data);

    const text = await callGemini(geminiKey.value(), {
      ...SETTINGS.generalChat,
      contents: [
        ...cleanHistory(history),
        { role: "user", parts: [{ text: buildUserMessage(query, selectedText || undefined) }] },
      ],
      systemInstruction: buildChatPrompt(ctx, "knowledge", task(ctx)),
    });

    return { response: text };
  }
);
