import { GoogleGenerativeAI, Content } from "@google/generative-ai";
import { HttpsError } from "firebase-functions/v2/https";

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
  });

  try {
    const result = await model.generateContent({
      contents,
      generationConfig: {
        temperature,
        maxOutputTokens,
      },
    });

    const response = await result.response;
    const text = response.text();

    if (!text) {
      throw new HttpsError("internal", "No text content in Gemini response");
    }

    return text;
  } catch (error: any) {
    console.error("Gemini SDK Error:", error);

    // Check for quota or other specific errors if possible via SDK error objects
    if (error.message?.includes("429") || error.status === 429) {
      throw new HttpsError("resource-exhausted", "Gemini API quota exceeded");
    }

    throw new HttpsError(
      "internal",
      `Gemini API failed: ${error.message || "Unknown SDK error"}`
    );
  }
};
