import {
  GoogleGenerativeAI,
  Content,
  HarmBlockThreshold,
  HarmCategory,
  SafetySetting,
} from "@google/generative-ai";
import { HttpsError } from "firebase-functions/v2/https";

/**
 * The adjustable filters, loosened to block only high-risk content. Quill's books are literature,
 * and the strict defaults refuse ordinary scenes of war, murder or cruelty. Gemini's non-adjustable
 * PROHIBITED_CONTENT filter can't be loosened here and still applies.
 */
const SAFETY_SETTINGS: SafetySetting[] = [
  HarmCategory.HARM_CATEGORY_HARASSMENT,
  HarmCategory.HARM_CATEGORY_HATE_SPEECH,
  HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
  HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
].map((category) => ({ category, threshold: HarmBlockThreshold.BLOCK_ONLY_HIGH }));

/**
 * The finish reasons that mean a safety filter stopped the answer. Plain strings: the API sends
 * newer reasons (PROHIBITED_CONTENT, BLOCKLIST, SPII) than this SDK version's enum knows.
 */
const BLOCK_REASONS = new Set<string>(["SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII"]);

/**
 * The error for a request Gemini's safety filters refused. The app recognises it by the code and
 * `details.reason` and shows a clear message instead of "Something went wrong".
 */
export const contentBlockedError = (reason: string) =>
  new HttpsError(
    "failed-precondition",
    `Gemini declined this request (${reason})`,
    { reason: "content_blocked", blockReason: reason }
  );

export const callGemini = async (
  key: string,
  modelName: string,
  contents: Content[],
  systemInstruction?: string,
  temperature = 0.4,
  maxOutputTokens = 4096
): Promise<string> => {
  const genAI = new GoogleGenerativeAI(key);
  const model = genAI.getGenerativeModel({
    model: modelName,
    systemInstruction: systemInstruction,
    safetySettings: SAFETY_SETTINGS,
  });

  try {
    const result = await model.generateContent({
      contents,
      generationConfig: {
        temperature,
        maxOutputTokens,
      },
    });

    const response = result.response;

    // A refusal: the prompt itself was blocked, or the answer was stopped by a safety filter
    const promptBlock = response.promptFeedback?.blockReason;
    const finishReason = response.candidates?.[0]?.finishReason;
    const blockReason =
      promptBlock ?? (finishReason && BLOCK_REASONS.has(finishReason) ? finishReason : undefined);
    if (blockReason) {
      console.warn("Gemini blocked the request:", {
        blockReason,
        safetyRatings: response.candidates?.[0]?.safetyRatings ?? response.promptFeedback?.safetyRatings,
      });
      throw contentBlockedError(String(blockReason));
    }

    const text = response.text();

    if (!text) {
      throw new HttpsError("internal", "No text content in Gemini response");
    }

    return text;
  } catch (error: any) {
    // Already a deliberate error (a block, or the empty-response check above)
    if (error instanceof HttpsError) {
      throw error;
    }

    console.error("Gemini SDK Error:", error);

    if (error.message?.includes("429") || error.status === 429) {
      throw new HttpsError("resource-exhausted", "Gemini API quota exceeded");
    }

    throw new HttpsError(
      "internal",
      `Gemini API failed: ${error.message || "Unknown SDK error"}`
    );
  }
};
