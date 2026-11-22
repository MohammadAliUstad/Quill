import {
  Content,
  GoogleGenAI,
  HarmBlockThreshold,
  HarmCategory,
  SafetySetting,
  ThinkingLevel,
} from "@google/genai";
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

/** The finish reasons that mean a safety filter stopped the answer. */
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

export interface GeminiRequest {
  model: string;
  contents: Content[];
  systemInstruction?: string;
  thinkingLevel: ThinkingLevel;
  /** Covers the model's thinking as well as the answer, so leave room for both. */
  maxOutputTokens: number;
  /** Plain JSON out, for the router. */
  json?: boolean;
}

/**
 * One Gemini call. Temperature is left at the model's default: Gemini 3 models are tuned for it,
 * and Google advises against lowering it.
 */
export const callGemini = async (key: string, request: GeminiRequest): Promise<string> => {
  const ai = new GoogleGenAI({ apiKey: key });

  try {
    const response = await ai.models.generateContent({
      model: request.model,
      contents: request.contents,
      config: {
        systemInstruction: request.systemInstruction,
        safetySettings: SAFETY_SETTINGS,
        maxOutputTokens: request.maxOutputTokens,
        thinkingConfig: { thinkingLevel: request.thinkingLevel },
        ...(request.json ? { responseMimeType: "application/json" } : {}),
      },
    });

    // A refusal: the prompt itself was blocked, or the answer was stopped by a safety filter
    const promptBlock = response.promptFeedback?.blockReason;
    const finishReason = String(response.candidates?.[0]?.finishReason ?? "");
    const blockReason =
      promptBlock ?? (BLOCK_REASONS.has(finishReason) ? finishReason : undefined);
    if (blockReason) {
      console.warn("Gemini blocked the request:", {
        blockReason,
        safetyRatings: response.candidates?.[0]?.safetyRatings ?? response.promptFeedback?.safetyRatings,
      });
      throw contentBlockedError(String(blockReason));
    }

    const text = response.text?.trim();
    if (!text) {
      // The finish reason goes in the message: the app shows its own note for MAX_TOKENS
      throw new HttpsError("internal", `No text content in Gemini response (${finishReason || "unknown"})`);
    }
    return text;
  } catch (error: any) {
    // Already a deliberate error (a block, or the empty-response check above)
    if (error instanceof HttpsError) {
      throw error;
    }

    console.error("Gemini SDK error:", error);

    if (error?.status === 429 || error?.message?.includes("429")) {
      throw new HttpsError("resource-exhausted", "Gemini API quota exceeded");
    }

    throw new HttpsError("internal", `Gemini API failed: ${error?.message || "Unknown SDK error"}`);
  }
};
