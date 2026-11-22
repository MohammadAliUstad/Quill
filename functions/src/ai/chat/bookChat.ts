import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";
import { SETTINGS } from "../../core/models";
import {
  buildChatPrompt,
  cleanHistory,
  cleanPassages,
  ReaderContext,
  readerContextFrom,
} from "../prompts/aira";

const geminiKey = defineSecret("GEMINI_API_KEY");

// For intents whose passages can span "early suspicion" and "later reveal": a novel's mysteries
// are usually resolved well after the event itself.
const CONFLICT_RULE = `
If the passages conflict, earlier suspicion or speculation against a later confession, discovery
or plain statement, the later one is the true answer. Say it clearly; don't hedge between the two
or present a resolved mystery as still open. If the passages hold no resolution, say so instead
of guessing.
`.trim();

// For anything about a particular person: keeps one character's facts off another's.
const ATTRIBUTION_RULE = `
Don't give anyone an action, trait or relationship the passages don't show as theirs; a detail
about someone else in the same passage (a parent, a sibling) isn't theirs. Use the exact names
the passages give. If a detail involves someone the passages never name, state the trait it
shows instead of pointing at an unnamed stand-in ("she'll confront people to protect her family",
not "she confronted a merchant").
`.trim();

// What each kind of question needs, on top of the shared base. The retrieval feeding each one is
// specialised per intent on the client (BookChatRepositoryImpl), so these match what it sends.
const INTENT_TASKS: Record<string, string> = {
  chapter_summary: `
They want a summary of the chapter they're on, and the passage given is that whole chapter. Tell
it in your own words, not phrases lifted from the text: the scenes it moves through, what the
people in it do and say that matters, and where it leaves things. Someone who skipped the chapter
should come away knowing what really happened. Usually a few paragraphs, following the chapter's
own movement.`,

  character_profile: `
They want to know who a character fundamentally is: personality, role in the story, key
relationships. This is a portrait, not a scene-by-scene account; if you're about to describe one
moment ("he blushed when..."), say the trait it reveals instead. Read every passage first and
decide what actually defines them.

${ATTRIBUTION_RULE}`,

  character_recent: `
They want to know what's going on for a character right now, not who they are in general or
their backstory. Group what's happened into the few developments that matter to where things
stand, rather than narrating every small action.

${ATTRIBUTION_RULE}`,

  character_arc: `
They want to trace how a character has changed over the story so far. Walk through the distinct
turns in order (early on, then, more recently), choosing only the moments that changed something
for them. It's a path through time, not a portrait; write plainly, not like an essay.

${ATTRIBUTION_RULE}`,

  relationship: `
They're asking about the relationship between two or more people. Give a real read on it:
mutual or one-sided, warm or wary, changing or fixed, and what shows it. Draw across the
passages rather than retelling one incident between them.

${ATTRIBUTION_RULE}`,

  plot_event: `
They're asking what happened, or why. Explain the events clearly and in order. If the passages
span more than one moment, connect them into one explanation instead of answering from the first
one you notice. For a "who did it" question, a confession or reveal in the passages is the
answer: say it plainly.

${CONFLICT_RULE}`,

  theme_analysis: `
They're asking about a theme, motif or symbol. Show how it actually works in this book, through
specific moments, not a literary-essay answer that could fit any novel. If it shows up in
several distinct ways, cover the well-supported ones and leave out weak ones.`,

  significance: `
They're asking why a particular moment or detail matters. Explain its effect on the characters,
the plot or the themes, grounded in what's shown. If its importance rests on something only
hinted at, call it a hint rather than a fact.`,

  quote_lookup: `
They're asking about specific wording or a quote. Precision is the point: quote the line exactly
(or as closely as the passages allow), say who says it and when, and add a sentence of context
if it helps. Keep it short.`,
};

const GENERIC_TASK = `
Answer from the passages, connecting what they show into the most complete answer they support.

${CONFLICT_RULE}`;

const buildSystemPrompt = (ctx: ReaderContext, queryIntent?: string): string => {
  const task = (queryIntent && INTENT_TASKS[queryIntent]) || GENERIC_TASK;
  return buildChatPrompt(ctx, "passages", task.trim());
};

const buildUserMessage = (context: unknown, query: string, selectedText?: string): string => `
${
  selectedText
    ? `HIGHLIGHTED PASSAGE (what the reader picked out; answer with this in mind first):\n${selectedText}\n\n`
    : ""
}PASSAGES:
${cleanPassages(context) || "(No relevant passages found.)"}

QUESTION:
${query}
`.trim();

export const bookChat = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { query, context, history, queryIntent, selectedText } = request.data;

    if (!query) {
      throw new HttpsError("invalid-argument", "Query is required");
    }

    const ctx = readerContextFrom(request.data);

    const text = await callGemini(geminiKey.value(), {
      ...SETTINGS.bookChat,
      contents: [
        ...cleanHistory(history),
        { role: "user", parts: [{ text: buildUserMessage(context, query, selectedText || undefined) }] },
      ],
      systemInstruction: buildSystemPrompt(ctx, queryIntent),
    });

    return { response: text };
  }
);
