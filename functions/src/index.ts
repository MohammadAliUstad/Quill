import { setGlobalOptions } from "firebase-functions";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import { defineSecret } from "firebase-functions/params";
import { TextToSpeechClient } from "@google-cloud/text-to-speech";

initializeApp();
setGlobalOptions({ maxInstances: 10 });

const geminiKey = defineSecret("GEMINI_API_KEY");

async function callGemini(
  apiKey: string,
  prompt: string,
  systemPrompt: string,
  history: { role: string; parts: { text: string }[] }[]
): Promise<string> {


  let cleanHistory = [...history];
  if (cleanHistory.length > 0 && cleanHistory[cleanHistory.length - 1].role === "user") {
    cleanHistory.pop();
  }

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

export const airaExpand = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { prompt } = request.data;

    if (!prompt) {
      throw new HttpsError("invalid-argument", "Prompt is required");
    }

    const text = await callGemini(geminiKey.value(), prompt, "", []);

    const variants = text
      .split("\n")
      .map((l: string) => l.trim())
      .filter((l: string) => l.length > 0)
      .slice(0, 3);

    return { variants };
  }
);

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

const ttsClient = new TextToSpeechClient();

export const airaTts = onCall(
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    // Removed accentCode from here
    const { text } = request.data;

    if (!text || typeof text !== "string") {
      throw new HttpsError("invalid-argument", "Text is required");
    }

    if (text.length > 5000) {
      throw new HttpsError("invalid-argument", "Text too long. Maximum 5000 characters.");
    }

    // Removed the unused languageCode variable entirely

    const [response] = await ttsClient.synthesizeSpeech({
      input: { text },
      voice: {
        languageCode: "en-US",
        name: "en-US-Studio-O",
      },
      audioConfig: {
        audioEncoding: "MP3",
      },
    });

    if (!response.audioContent) {
      throw new HttpsError("internal", "No audio generated");
    }

    const audioBase64 = Buffer.from(response.audioContent as Uint8Array).toString("base64");

    return { audio: audioBase64 };
  }
);