import { onCall, HttpsError } from "firebase-functions/v2/https";
import { SecretParam } from "firebase-functions/params";

export const createBookChat = (geminiKey: SecretParam) => onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { prompt, context, bookTitle, bookAuthor, history } = request.data;

    if (!prompt) {
      throw new HttpsError("invalid-argument", "Prompt is required");
    }

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

    const apiKey = geminiKey.value();
    let cleanHistory = [...(history ?? [])];
    if (cleanHistory.length > 0 && cleanHistory[cleanHistory.length - 1].role === "user") {
      cleanHistory.pop();
    }

    const requestBody: any = {
      contents: [
        ...cleanHistory,
        { role: "user", parts: [{ text: userPrompt }] }
      ],
      generationConfig: {
        temperature: 0.4,
        maxOutputTokens: 4096
      },
      system_instruction: {
        parts: [{ text: systemPrompt }]
      }
    };

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${apiKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody)
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      console.error("BOOK CHAT - GEMINI REJECTED:", JSON.stringify(errorData));
      if (response.status === 429) {
        throw new HttpsError("resource-exhausted", "Gemini quota exceeded");
      }
      throw new HttpsError("internal", `Gemini error: ${errorData.error?.message || response.statusText}`);
    }

    const data = await response.json();
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!text) {
      throw new HttpsError("internal", "Empty response from Gemini");
    }

    return { response: text };
  }
);
