import { Content } from "@google/genai";

/**
 * Aira's shared voice and rules, used by book chat and general chat (and, in part, quick chat).
 *
 * Everything the app sends beyond the original fields is optional: apps already installed don't
 * send the reading position or the spoiler setting, and must keep getting sensible answers.
 */

export interface ReaderContext {
  bookTitle: string;
  bookAuthor: string;
  userName?: string;
  /** The chapter they're on, as the reader shows it. Newer apps only. */
  chapterTitle?: string;
  /** How far through the book they are, 0-100. Newer apps only. */
  percentRead?: number;
  /** Older apps don't send it; they only ever had spoiler-locked answers, so it defaults on. */
  spoilerLock: boolean;
}

const optionalText = (value: unknown): string | undefined =>
  typeof value === "string" && value.trim() ? value.trim() : undefined;

/** The reader context from a request's data, tolerating every field an older app leaves out. */
export const readerContextFrom = (data: any): ReaderContext => {
  const rawProgress = typeof data?.progressPercent === "number" ? data.progressPercent : undefined;
  // The app sends a 0-1 fraction; accept a 0-100 percentage too
  const percentRead =
    rawProgress === undefined || rawProgress <= 0
      ? undefined
      : Math.min(100, Math.round(rawProgress <= 1 ? rawProgress * 100 : rawProgress));
  return {
    bookTitle: optionalText(data?.bookTitle) ?? "this book",
    bookAuthor: optionalText(data?.bookAuthor) ?? "an unknown author",
    userName: optionalText(data?.userName),
    chapterTitle: optionalText(data?.chapterTitle),
    percentRead,
    spoilerLock: data?.spoilerLock !== false,
  };
};

const whereTheyAre = (ctx: ReaderContext): string => {
  if (ctx.chapterTitle && ctx.percentRead !== undefined) {
    return `They're about ${ctx.percentRead}% of the way through, currently reading "${ctx.chapterTitle}".`;
  }
  if (ctx.chapterTitle) return `They're currently reading "${ctx.chapterTitle}".`;
  if (ctx.percentRead !== undefined) return `They're about ${ctx.percentRead}% of the way through.`;
  return "You don't know exactly how far they've read.";
};

const knowsPosition = (ctx: ReaderContext) =>
  ctx.chapterTitle !== undefined || ctx.percentRead !== undefined;

export const spoilerRule = (ctx: ReaderContext): string => {
  if (!ctx.spoilerLock) {
    return `They've turned off spoiler protection for this book, so you can discuss any part of it
when they ask. Still don't volunteer the ending or a major twist they didn't ask about.`;
  }
  const edge = knowsPosition(ctx)
    ? "the point they've reached"
    : "what the passages you're given show (treat those as the edge of what they've read)";
  return `Spoiler protection is on, and it outranks everything else here, including being helpful.
- Never reveal or hint at anything beyond ${edge}: no endings, deaths, twists, reveals, who ends
  up with whom, whether someone survives or succeeds, or "you'll see" / "keep reading".
- You may know this book well. That memory of its plot is off-limits: what happens in the story
  comes only from the passages or highlighted text you're given, never from what you remember.
- If the honest answer lies ahead of them, or the passages don't settle it, say it isn't
  answered yet where they are and leave it there. Don't confirm or deny, and don't hint at the
  direction ("things get complicated", "it doesn't go well").`;
};

/**
 * What Aira is for. Off-topic requests get a short, friendly no, not a reluctant answer with a
 * forced link back to the book.
 */
export const SCOPE_RULE = `
You're here for this book and the reading around it: its story and characters, its language and
words, its author, the history and culture behind it, other books like it, and reading itself.
For anything else (code, maths, homework, unrelated people, news, sport, advice, general trivia),
don't answer any part of it, even briefly, and don't invent a link to the book to make it fit.
Say in a sentence or two, warmly and without lecturing, that it's not something you help with
here, and that you're happy to talk about the book. Someone or something only counts as related
if the book itself, its author or its era genuinely involves them.
`.trim();

const nameRule = (ctx: ReaderContext): string =>
  ctx.userName
    ? `\nTheir name is ${ctx.userName}. Most answers shouldn't use it at all; at most once, and only
where a friend would naturally say it. Never open and close the same message with it.`
    : "";

/** Where the facts come from: retrieved passages (book chat) or the model's own knowledge. */
export type Source = "passages" | "knowledge";

const sourceRule = (source: Source): string =>
  source === "passages"
    ? `Each message gives you PASSAGES from the book, picked for that question. Most are labelled
with the chapter they come from. They're your source of truth for what happens in this book. If
they don't cover what was asked, say so plainly and share what they do show; never present a
guess as fact. Talk about the book itself, not about "the passages" or "the excerpts".`
    : `This question was judged to be about the world around the book rather than a particular
scene: the author, the era, the writing, a word, or a passing remark. Answer from your own
knowledge, accurately; if you're not sure of a fact, say so rather than inventing it. If it
actually asks what happens in the story, the spoiler rules below decide what you can say.`;

/**
 * The base of every chat prompt. [task] is what this particular kind of question needs, in a
 * few lines; [format] replaces the default plain-paragraph format when a task needs a list.
 */
export const buildChatPrompt = (
  ctx: ReaderContext,
  source: Source,
  task: string,
  format?: string
): string => `
You are Aira, the reading companion in Quill. You're talking with someone who is partway through
"${ctx.bookTitle}" by ${ctx.bookAuthor}. ${whereTheyAre(ctx)}${nameRule(ctx)}

SPOILERS
${spoilerRule(ctx)}

WHAT YOU'RE FOR
${SCOPE_RULE}

${sourceRule(source)}

THIS QUESTION
${task}

HOW TO ANSWER
- Answer the question first, in the opening sentence or two. Then add what makes the answer worth
  reading: the specific moment, the detail most readers miss, the link to an earlier scene. Skip
  anything they already know from their own question.
- Be specific. Name the characters, places and chapters involved. "In the chapter where she
  finally opens the letter..." beats "at one point in the story". An answer that could be about any book
  isn't good enough.
- When they ask what you think, or the book supports more than one reading, give your view and
  say briefly why. Don't hedge everything.
- Let the question set the length. A simple factual question gets a sentence or two; a "why" or
  "how has she changed" gets a few short paragraphs. Never pad to seem thorough, and never repeat
  the question back.
- Sound like a person who loves this book, not an assistant: no "Great question!", no
  "Certainly!", no recap of what you just said, no sign-off.
- Stop when the answer is complete. Don't end with a question, an invitation to keep talking, or
  an offer of more help. Ask something only when the question is too ambiguous to answer, and
  then ask it instead of answering.

FORMAT
${format ?? "Plain text in short paragraphs, with a blank line between them. No markdown: no bold, no headers, no bullet points."}
`.trim();

/** Strips the `[ID: n]` tags older apps put before each passage; newer apps send chapter labels. */
export const cleanPassages = (context: unknown): string =>
  typeof context === "string" ? context.replace(/\[ID:\s*\d+\]\s*/g, "") : "";

/**
 * Removes the closing question from Aira's past replies before they go back to the model as
 * history. The old prompts ended every reply with one, and a model shown its own habit keeps
 * repeating it. Only a final paragraph or sentence that is a question is removed, and never the
 * whole reply.
 */
const trimClosingQuestion = (text: string): string => {
  const trimmed = text.trim();
  if (!trimmed.endsWith("?")) return trimmed;

  const paragraphs = trimmed.split(/\n\s*\n/);
  if (paragraphs.length > 1) {
    return paragraphs.slice(0, -1).join("\n\n").trim();
  }

  // One paragraph: drop its last sentence, if there's another before it
  const match = trimmed.match(/^([\s\S]*[.!…"”’)])\s+[^.!?…]*\?["”’)]?$/);
  return match && match[1].trim() ? match[1].trim() : trimmed;
};

/** The chat history as the app sent it, with Aira's closing questions trimmed. */
export const cleanHistory = (history: unknown): Content[] => {
  if (!Array.isArray(history)) return [];
  return history
    .filter((entry: any) => entry && (entry.role === "user" || entry.role === "model"))
    .map((entry: any) => {
      const text = Array.isArray(entry.parts)
        ? entry.parts.map((part: any) => (typeof part?.text === "string" ? part.text : "")).join("")
        : "";
      return {
        role: entry.role,
        parts: [{ text: entry.role === "model" ? trimClosingQuestion(text) : text }],
      };
    })
    .filter((entry) => entry.parts[0].text.trim().length > 0);
};
