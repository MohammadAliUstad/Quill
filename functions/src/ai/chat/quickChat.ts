import { onCall, HttpsError } from "firebase-functions/v2/https";
import { SecretParam } from "firebase-functions/params";
import { callGemini } from "../core/gemini";

type PromptBuilder = (bookTitle: string, bookAuthor: string, query?: string) => string;

const ACTION_PROMPTS: Record<string, PromptBuilder> = {
  SUMMARIZE_CHAPTER: (t, a) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nSummarize the following chapter text in 3-4 sentences.\nFocus on the key events, character actions, and any important revelations.\nUse plain text only. No markdown, no bold, no headers.`,

  WHO_ARE_CHARACTERS: (t, a) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nBased only on the passages provided, list the key characters the reader has encountered so far.\nFor each, give their name and one brief sentence describing who they are.\nUse plain text only. No markdown, no bold.`,

  WHO_IS_THIS: (t, a, q) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nBased ONLY on the passages provided, explain who "${q}" is.\nDescribe their role, personality, or relationship to other characters as shown in what the reader has read so far.\nDo NOT reveal anything beyond what the passages contain. Keep it to 3-4 sentences.\nUse plain text only. No markdown, no bold.`,

  WHAT_ARE_THEMES: (t, a) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nBased only on the passages provided, identify the main themes present in what the reader has read so far.\nKeep the answer to 3-4 sentences.\nUse plain text only. No markdown, no bold, no headers.`,

  DEFINE_WORD: () =>
    `You are a precise dictionary. Give a concise definition of the word provided.\nInclude the part of speech and one example sentence if helpful.\nKeep it to 2-3 sentences maximum. Use plain text only.`,

  WHAT_IS_THIS: (t, a) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nExplain what this word or term refers to — it may be a place, a dish, an object, a social custom, or a historical concept from the book's era.\nKeep the explanation to 2-3 sentences. Use plain text only.`,

  SIMPLIFY_THIS: (t, a) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nRewrite the following selection in clear, plain modern English.\nPreserve the meaning exactly — just make it easier to understand.\nDo not summarize or shorten it significantly. Use plain text only.`,

  EXPLAIN_THIS: (t, a) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nExplain what the following selection means in context.\nWhat is being said or implied? What literary or historical context is relevant?\nKeep it to 3-4 sentences. Use plain text only.`,

  WHAT_SIGNIFICANCE: (t, a) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nBased on the highlighted passage and the surrounding context provided, explain why this moment is significant to the story — its impact on characters, plot, or themes.\nKeep it to 3-4 sentences. Plain text only.`,

  WHO_IS_SPEAKING: (t, a) =>
    `You are Aira, a reading companion for "${t}" by ${a}.\nBased on the highlighted passage and surrounding context, identify who is speaking or narrating.\nIf it is dialogue, say who said it and to whom. If it is narration, say whose perspective it is.\nKeep it to 2-3 sentences. Use plain text only.`,
};

export const createQuickChat = (geminiKey: SecretParam) => onCall(
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

    const finalQuery = `
      CONTEXT:
      ${context}

      QUESTION/TEXT:
      ${query || "Process the context above based on your instructions."}
    `.trim();

    const text = await callGemini(geminiKey.value(), {
      contents: [{ role: "user", parts: [{ text: finalQuery }] }],
      generationConfig: { temperature: 0.4, maxOutputTokens: 1024 },
      system_instruction: { parts: [{ text: systemPrompt }] },
    });

    return { response: text.trim() };
  }
);