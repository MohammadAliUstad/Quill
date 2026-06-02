import { onCall, HttpsError } from "firebase-functions/v2/https";
import { SecretParam } from "firebase-functions/params";

export const createDetectIntent = (geminiKey: SecretParam) => onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { prompt, bookTitle, bookAuthor } = request.data;

    if (!prompt) {
      throw new HttpsError("invalid-argument", "Prompt is required");
    }

    const routerPrompt = `
    You are a query classifier for a book reading app.
    The user is currently reading "${bookTitle}" by ${bookAuthor}.

    Classify whether the following question requires retrieving specific passages from the book, or can be answered from general knowledge.

    RAG is required when the question asks about:
    - Specific events, scenes, or moments in the book
    - Character actions, dialogue, or relationships within the story
    - Plot details or story progression
    - Direct quotes or specific wording from the book

    RAG is NOT required when the question asks about:
    - General information about the book (synopsis, themes, genre)
    - The author's background or other works
    - Historical or cultural context around the book
    - General literary discussion or recommendations
    - Casual conversation or greetings

    If RAG is required, also extract:
    - entities: all character names, place names, and proper nouns mentioned in the question, expanded to their full formal names if you know them from the book
    - keywords: the most meaningful content words from the question, excluding stop words, that would likely appear verbatim in the relevant passage
    - queryIntent: one of character_info, relationship, plot_event, quote_lookup, general

    Respond ONLY with a valid JSON object. No preamble, no explanation, no markdown.

    If RAG is required:
    {
      "isRAG": true,
      "queryVariations": [
        "variation one as a declarative phrase",
        "variation two from a different angle",
        "variation three emphasizing a different aspect"
      ],
      "entities": ["Full Name One", "Full Name Two"],
      "keywords": ["keyword1", "keyword2", "keyword3"],
      "queryIntent": "plot_event"
    }

    If RAG is not required:
    {
      "isRAG": false,
      "queryVariations": [],
      "entities": [],
      "keywords": [],
      "queryIntent": "general"
    }

    Question: ${prompt}
    `.trim();

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${geminiKey.value()}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ role: "user", parts: [{ text: routerPrompt }] }],
          generationConfig: {
            temperature: 0.1,
            maxOutputTokens: 256
          }
        })
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      console.error("DETECT INTENT - GEMINI REJECTED:", JSON.stringify(errorData));
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
