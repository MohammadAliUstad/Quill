import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { GoogleGenAI, Modality } from "@google/genai";

const geminiKey = defineSecret("GEMINI_API_KEY");

const IMAGE_MODEL = "gemini-2.5-flash-image";

export const visualizeScene = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { bookTitle, bookAuthor, passage } = request.data;

    if (!passage || typeof passage !== "string" || !passage.trim()) {
      throw new HttpsError("invalid-argument", "passage is required");
    }

    const prompt = `
You will be given a passage from the book "${bookTitle}" by ${bookAuthor}. Generate a single
illustration based only on what the passage explicitly describes -- this may be a person's
physical appearance and demeanor, a setting/place/environment, or people within a place. Do not
add anything you know about this book or its characters from outside this passage -- if a detail
isn't stated, don't invent it.

STYLE: Render this as a classical painterly illustration -- rich oil-painting textures, visible
painterly brushwork, warm and slightly muted tones, soft directional lighting. Think timeless
book illustration or classical portraiture, not tied to any one country or period's painting
tradition. Do not render it as a photograph, a photorealistic image, a cartoon, an anime/comic
style, or a flat digital illustration -- it should look painted.

COMPOSITION: The painting must fill the entire canvas edge to edge. Do not leave any blank,
white, or solid-color margins, borders, frames, or letterbox/pillarbox bars around the scene --
extend the background environment to every edge instead.

If the passage genuinely doesn't contain enough descriptive detail to illustrate (e.g. it's pure
dialogue or internal thought, with no physical description of a person or place), do not generate
an image -- respond with a brief, plain explanation of why instead.

PASSAGE:
"""
${passage}
"""
`.trim();

    const ai = new GoogleGenAI({ apiKey: geminiKey.value() });

    try {
      const response = await ai.models.generateContent({
        model: IMAGE_MODEL,
        contents: prompt,
        config: {
          // gemini-2.5-flash-image is a conversational model that can also emit images --
          // requesting IMAGE alone is a known failure mode for this model family (it expects
          // to be allowed to respond with text too, even if that text ends up minimal).
          responseModalities: [Modality.TEXT, Modality.IMAGE],
          // Without an explicit ratio the model defaults to a square canvas, and when it paints
          // a tall subject (e.g. a full-length figure) it pads the leftover width with white
          // bars. Fixing a portrait ratio matches the peek card and removes that filler.
          imageConfig: { aspectRatio: "3:4" },
        },
      });

      const parts = response.candidates?.[0]?.content?.parts ?? [];
      const imagePart = parts.find((part) => part.inlineData);
      const base64Data = imagePart?.inlineData?.data;
      const mimeType = imagePart?.inlineData?.mimeType ?? "image/png";

      if (!base64Data) {
        // Log everything useful for diagnosing *why* no image came back -- the model's own
        // text (often a refusal or clarification), the finish reason, and safety ratings --
        // so a future failure doesn't require another blind guess-and-redeploy cycle.
        console.error("No image part in Gemini response.", {
          finishReason: response.candidates?.[0]?.finishReason,
          safetyRatings: response.candidates?.[0]?.safetyRatings,
          promptFeedback: response.promptFeedback,
          responseText: response.text,
          partsSummary: parts.map((part) => Object.keys(part)),
        });

        // A clean STOP with no image is the model declining because the passage genuinely
        // has nothing visual to draw (per its own instructions) -- not a technical failure.
        // Use a distinct error code so the client can show a friendly, specific message
        // instead of a generic "something went wrong."
        throw new HttpsError(
          "failed-precondition",
          "This passage doesn't have enough visual detail to generate an image. Try selecting text that vividly describes a person or a place."
        );
      }

      return { imageBase64: base64Data, mimeType };
    } catch (error: any) {
      console.error("Gemini image generation error:", error);

      if (error instanceof HttpsError) {
        throw error;
      }

      if (error.message?.includes("429") || error.status === 429) {
        throw new HttpsError("resource-exhausted", "Gemini API quota exceeded");
      }

      throw new HttpsError(
        "internal",
        `Image generation failed: ${error.message || "Unknown SDK error"}`
      );
    }
  }
);
