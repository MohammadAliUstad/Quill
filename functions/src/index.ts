import { setGlobalOptions } from "firebase-functions";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import { defineSecret } from "firebase-functions/params";

initializeApp();
setGlobalOptions({ maxInstances: 10 });

const geminiKey = defineSecret("GEMINI_API_KEY");

async function callGemini(
  apiKey: string,
  prompt: string,
  systemPrompt: string,
  history: { role: string; parts: { text: string }[] }[]
): Promise<string> {

  // FIX 1: Clean up broken history.
  // If the Android DB sends history that ends with a "user" (because a previous AI reply failed to save),
  // we remove it so we don't send "user" -> "user" consecutively.
  let cleanHistory = [...history];
  if (cleanHistory.length > 0 && cleanHistory[cleanHistory.length - 1].role === "user") {
    cleanHistory.pop();
  }

  // Build the base request body
  const requestBody: any = {
    contents: [
      ...cleanHistory,
      { role: "user", parts: [{ text: prompt }] }
    ],
    generationConfig: {
      temperature: 0.4,
      maxOutputTokens: 4096
    }
  };

  // FIX 2: Only attach system_instruction if it actually has content.
  if (systemPrompt && systemPrompt.trim().length > 0) {
    requestBody.system_instruction = {
      parts: [{ text: systemPrompt.trim() }]
    };
  }

  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${apiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestBody)
    }
  );

  if (!response.ok) {
    // FIX 3: Detailed Error Logging so Logcat tells you exactly what went wrong
    const errorData = await response.json().catch(() => ({}));
    console.error("GEMINI API REJECTED PAYLOAD:", JSON.stringify(errorData));

    if (response.status === 429) {
      throw new HttpsError("resource-exhausted", "Gemini quota exceeded");
    }

    const detailedMessage = errorData.error?.message || response.statusText;
    throw new HttpsError("internal", `Gemini error: ${detailedMessage}`);
  }

  const data = await response.json();
  const text = data.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!text) {
    throw new HttpsError("internal", "Empty response from Gemini");
  }

  return text;
}

// ── airaChat ──────────────────────────────────────────────────────────────────
export const airaChat = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { prompt, systemPrompt, history } = request.data;

    if (!prompt) {
      throw new HttpsError("invalid-argument", "Prompt is required");
    }

    const text = await callGemini(
      geminiKey.value(),
      prompt,
      systemPrompt ?? "",
      history ?? []
    );

    return { response: text.split("**").join("").split("*").join("") };
  }
);

// ── airaExpand ────────────────────────────────────────────────────────────────
export const airaExpand = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    // Receive the full prompt built by Kotlin
    const { prompt } = request.data;

    if (!prompt) {
      throw new HttpsError("invalid-argument", "Prompt is required");
    }

    // Call Gemini (no system prompt or history needed for expansion)
    const text = await callGemini(geminiKey.value(), prompt, "", []);

    const variants = text
      .split("\n")
      .map((l: string) => l.trim())
      .filter((l: string) => l.length > 0)
      .slice(0, 3);

    return { variants };
  }
);

// ── airaRoute ────────────────────────────────────────────────────────────────
export const airaRoute = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { prompt } = request.data;

    if (!prompt) {
      throw new HttpsError("invalid-argument", "Prompt is required");
    }

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${geminiKey.value()}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ role: "user", parts: [{ text: prompt }] }],
          generationConfig: {
            temperature: 0.1,
            maxOutputTokens: 256
          }
        })
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      console.error("AIRA ROUTE - GEMINI REJECTED:", JSON.stringify(errorData));
      throw new HttpsError("internal", `Gemini error: ${errorData.error?.message}`);
    }

    const data = await response.json();
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!text) {
      throw new HttpsError("internal", "Empty response from Gemini");
    }

    return { response: text.trim() };
  }
);