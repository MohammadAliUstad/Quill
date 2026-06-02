import { HttpsError } from "firebase-functions/v2/https";

export async function callGemini(
  apiKey: string,
  prompt: string,
  systemPrompt: string,
  history: { role: string; parts: { text: string }[] }[]
): Promise<string> {
  const cleanHistory = [...history];
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
