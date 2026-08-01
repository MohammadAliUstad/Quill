import { onCall, HttpsError } from "firebase-functions/v2/https";
import { TextToSpeechClient } from "@google-cloud/text-to-speech";

const ttsClient = new TextToSpeechClient();

export const speakText = onCall(
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { text } = request.data;

    if (!text || typeof text !== "string") {
      throw new HttpsError("invalid-argument", "Text is required");
    }

    if (text.length > 5000) {
      throw new HttpsError("invalid-argument", "Text too long. Maximum 5000 characters.");
    }

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