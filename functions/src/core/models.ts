import { ThinkingLevel } from "@google/genai";

/**
 * Which Gemini model each Aira function uses, in one place.
 *
 * Before deploying, check both IDs against the model list in AI Studio: Google renames and
 * redirects Gemini models often, and a wrong ID fails every request.
 */

/** The answers people read: book chat and general chat. */
export const CHAT_MODEL = "gemini-3.8-flash";

/** Fast, well-defined jobs: the router and the quick-action chips. */
export const LITE_MODEL = "gemini-3.5-flash-lite";

/** Per function: how hard the model thinks, and the output budget (thinking counts toward it). */
export const SETTINGS = {
  bookChat: { model: CHAT_MODEL, thinkingLevel: ThinkingLevel.MEDIUM, maxOutputTokens: 8192 },
  generalChat: { model: CHAT_MODEL, thinkingLevel: ThinkingLevel.LOW, maxOutputTokens: 8192 },
  quickChat: { model: LITE_MODEL, thinkingLevel: ThinkingLevel.LOW, maxOutputTokens: 4096 },
  detectIntent: { model: LITE_MODEL, thinkingLevel: ThinkingLevel.MINIMAL, maxOutputTokens: 1024 },
} as const;
