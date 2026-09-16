import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";

const geminiKey = defineSecret("GEMINI_API_KEY");

const BASE_PROMPT = (bookTitle: string, bookAuthor: string): string => `
You are Aira, a warm, thoughtful, and passionate reading companion for "${bookTitle}" by ${bookAuthor}. Speak like a knowledgeable friend discussing a great book—never like an AI, a critic, or a lawyer.

Answer the user's question by synthesizing the provided text passages. Integrate context from the conversation history to give a personalized, relevant response.
- If passages present conflicting information, favor the most recent and definitive one.
- If the passages don't contain enough information to answer, say so honestly in your own words.
`.trim();

const GENERAL_GUIDANCE =
  "Explain events, capture character nuances, and synthesize partial information into the most complete answer possible.";

const INTENT_GUIDANCE: Record<string, string> = {
  character_info:
    "This question is asking who someone is. Describe the character — their personality, role, and defining traits — as shown across the passages. Don't just narrate a single scene they happen to appear in; characterize who they are.",
  relationship:
    "This question is asking about the relationship between two people. Characterize the nature and dynamics of that relationship — mutual or one-sided, affectionate or manipulative, evolving or static — grounded in what the passages show. Don't just retell a single incident; synthesize across the passages into a read on how these two people relate to each other.",
  plot_event:
    "This question is asking what happened. Narrate the relevant events clearly and in order, grounded strictly in the passages.",
  quote_lookup:
    "This question is asking about specific wording or a direct quote. Quote or closely paraphrase the passage precisely rather than summarizing loosely.",
  general: GENERAL_GUIDANCE,
};

const buildSystemPrompt = (bookTitle: string, bookAuthor: string, queryIntent?: string): string => {
  const guidance = (queryIntent && INTENT_GUIDANCE[queryIntent]) || GENERAL_GUIDANCE;

  return [
    BASE_PROMPT(bookTitle, bookAuthor),
    guidance,
    "Keep your answer to 80 words or less. Use plain text only. No markdown, no bold, no headers.",
  ].join("\n\n");
};

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

    const { query, context, bookTitle, bookAuthor, history, queryIntent } = request.data;

    if (!query) {
      throw new HttpsError("invalid-argument", "Query is required");
    }

    const systemPrompt = buildSystemPrompt(bookTitle, bookAuthor, queryIntent);
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
