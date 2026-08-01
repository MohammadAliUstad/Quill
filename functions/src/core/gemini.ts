import { HttpsError } from "firebase-functions/v2/https";

const GEMINI_ENDPOINT =
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";

export const callGemini = async (key: string, payload: any): Promise<string> => {
  const response = await fetch(`${GEMINI_ENDPOINT}?key=${key}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    console.error("Gemini API Error:", JSON.stringify(errorData));
    if (response.status === 429) {
      throw new HttpsError("resource-exhausted", "Gemini API quota exceeded");
    }
    throw new HttpsError(
      "internal",
      `Gemini API failed: ${errorData.error?.message || response.statusText}`
    );
  }

  const data = await response.json();
  const text: string | undefined = data.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!text) {
    throw new HttpsError("internal", "No text content in Gemini response");
  }

  return text;
};