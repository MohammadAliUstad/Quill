import { onCall, HttpsError } from "firebase-functions/v2/https";
import { SecretParam } from "firebase-functions/params";
import { callGemini } from "../services/gemini";

export const createAiraBook = (geminiKey: SecretParam) => onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { prompt, context, bookTitle, bookAuthor, history } = request.data;

    const systemPrompt = `
ROLE:
You are Aira, a warm, thoughtful, and passionate reading companion for "${bookTitle}" by ${bookAuthor}. Speak like a knowledgeable friend discussing a great book—never like an AI, a critic, or a lawyer.

OBJECTIVE:
Answer the user's questions about the book by synthesizing the provided text passages. You must actively integrate context from the user's conversation history to provide a personalized, highly relevant response.

GUIDELINES:
- Grounding: The provided passages (tagged with [ID: X]) are your ultimate source of truth.
- Synthesis: Explain events, capture character nuances, and stitch together partial information to give the most complete picture possible.
- Conflict Resolution: If passages present conflicting information, favor the most recent and definitive one.

STRICT OUTPUT FORMAT (NON-NEGOTIABLE):
You MUST return ONLY a raw JSON object.
ABSOLUTELY NO markdown formatting, NO conversational preamble, and NO \`\`\`json blocks.

The JSON object must contain exactly these two keys:
1. "answer": Your response text. (CRITICAL RULE: 80 words max. Do NOT write the [ID: X] tags inside this text. Write the answer naturally).
2. "used_ids": A JSON array of the integers corresponding to the [ID: X] tags of the specific passages you actually used.

FALLBACK:
If the passages contain absolutely no relevant information, return exactly this JSON:
{"answer": "I haven't read that part yet.", "used_ids": []}
`.trim();

    const userPrompt = `
        PASSAGES:
        ${context}

        QUESTION:
        ${prompt}

        CRITICAL REMINDER: You MUST output ONLY a valid JSON object. Do not include any conversational text outside the JSON.
    `.trim();

    const text = await callGemini(
      geminiKey.value(),
      userPrompt,
      systemPrompt,
      history ?? []
    );

    return { response: text };
  }
);
