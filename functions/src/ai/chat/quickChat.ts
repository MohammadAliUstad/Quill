import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { callGemini } from "../../core/gemini";

const geminiKey = defineSecret("GEMINI_API_KEY");

type PromptBuilder = (bookTitle: string, bookAuthor: string, query?: string) => string;

// Shared by the character actions (Who is this / Recent Role / Journey So Far). The chips are
// offered for any capitalized selection, so the tapped word can be a place, an object, or just a
// capitalized ordinary word -- without this check the model would invent a "character" for it.
const characterNameCheck = (query?: string) => `
FIRST, CHECK THE WORD: Before anything else, decide from the passages whether "${query}" is
actually the name of a character -- a person, or a named animal or being that acts like one. It
might instead be a place, an object, an organization, a title, or an ordinary word that just
happened to be capitalized.

If "${query}" is NOT a character's name, do not do the task below. Instead reply with only a
short, warm note of one or two sentences that tells the reader "${query}" doesn't seem to be a
character's name and invites them to select a character's name so you can help. Keep it light
and friendly, in your own words -- for example: "Hmm, "Ravenmoor" doesn't seem to be a
character's name. Try selecting a character's name and I'll tell you all about them!"

If "${query}" IS a character's name, skip this note entirely and do the task below.
`.trim();

const ACTION_PROMPTS: Record<string, PromptBuilder> = {
  SUMMARIZE_CHAPTER: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.

Write a comprehensive, 10-15 sentence summary of the chapter text below, as flowing prose — not
a list of facts. Don't lift and compress phrases straight from the text; read the whole chapter,
understand what actually happens in it, and describe that in your own words. Cover the scenes it
moves through, what each character does and says that matters, and how it develops or ends. A
reader who hasn't read the chapter should come away understanding what really happened in it.

Grounding rules:
- Base the summary only on what the chapter text explicitly says. Do not add events, outcomes, or details it does not contain.
- Attribute every action to the exact character the text names as doing it. If two characters are in the same scene, never extend one's action onto the other.
- If something is implied but not directly stated, either leave it out or say it is implied — never present an inference as a confirmed fact.
- Do not resolve an ambiguous moment by picking the most dramatic or "logical" reading. When in doubt, describe only what is certain.

Use plain text only. No markdown, no bold, no headers.
`.trim(),

  WHO_ARE_CHARACTERS: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Based only on the passages provided, list the key characters the reader has encountered so far.

For each, give their name, their role or relationship to the other characters, and -- when the
passages actually show it -- a personality trait or defining quality, in one to two sentences.
Don't just restate a single action from one scene ("he came to visit and asked a question") --
if the passages show a pattern or stable trait behind that action, say that instead (e.g. that
they're impulsive, calculating, or devoted to a sibling). Read across all their mentions before
writing about them, not just the first one you see.
If a name appears with no real description anywhere in the passages, say only that they have
been mentioned rather than inventing a personality or role for them.

FORMATTING: List each character on its own line, in the form "Name — description". Use plain
text only -- no markdown, no bold, no bullet characters (-, *, •), no headers.
`.trim(),

  RECALL_CHARACTER: (title, author, query) => `
You will be given a series of passages from the book "${title}" by ${author} that mention
"${query}".

${characterNameCheck(query)}

Using only what these passages say, write a character profile of "${query}" -- who they
fundamentally are: their personality, their role in the story, and their key relationships. This
is a profile, not a scene-by-scene account. Do not narrate individual moments, reactions, or
incidents one at a time -- if you catch yourself writing a sentence about a specific thing that
happened in one scene ("he blushed when...", "he was surprised that...", "he tasted the wine
and..."), stop. Either leave it out, or restate it as the stable trait or pattern it's evidence
of -- e.g. that moment might really be evidence that he's shy, or trusting, or self-controlled;
say that instead of narrating the moment itself. Read every passage first, decide what actually
defines this character, and write only that.

Do not assume or invent any action, event, trait, or relationship that the passages do not
explicitly attribute to "${query}" specifically -- a detail shown about someone else in the same
passage (a parent, a sibling, another character) is not "${query}"'s unless the passage names
"${query}" as the one it belongs to. If you do refer to another person, use the exact name the
passages give them -- never a vague reference like "a woman" or "someone" when the passages name
that person.

If a specific detail involves another person who ISN'T named anywhere in the passages you were
given, don't describe that detail through an unnamed stand-in -- doing so reads like you're
withholding a name the reader assumes you have. Instead, generalize the sentence to state the
trait or pattern the detail shows, without pointing at a specific unnamed person. For example, if
a passage shows someone confronting an unnamed merchant to protect her brother, don't write "she
confronted a rude merchant to protect her brother" -- write "she's shown she'll confront people
directly to protect her family" instead. The same goes for an unattributed quote: if a passage
has an unnamed villager remark that someone is "the bravest fool in town," don't write "one person
observed she was 'the bravest fool in town'" -- either use the real name if the passages give one
elsewhere, or paraphrase it as your own description: "others see her as reckless but admirable."

Do not use any outside knowledge you may have of this book or character -- only what appears in
the passages below.

Write as much as the passages actually support -- aim for 9-15 sentences when there's enough
substantive material, fewer if there genuinely isn't. Every sentence must earn its place: cover
only the most important, distinct facts and facets, don't pad by restating the same trait
different ways.

Use plain text only. No markdown, no bold, no headers, no bullet lists.
`.trim(),

  RECENT_ROLE: (title, author, query) => `
You will be given a series of passages from the book "${title}" by ${author} covering "${query}"'s
most recent appearances.

${characterNameCheck(query)}

Using only what these passages say, write a recap of what's currently happening for "${query}" and
how things stand for them right now -- not a general introduction to who they are or their
backstory. This is a recap of recent developments, not a moment-by-moment transcript -- if you
catch yourself writing a separate sentence for every small action or reaction, stop, and instead
group what's happened into the handful of developments that actually matter to their current
situation. Read every passage first, decide what's actually changed or is currently unfolding for
them, and write only that.

Do not assume or invent any action, event, trait, or relationship that the passages do not
explicitly attribute to "${query}" specifically -- a detail shown about someone else in the same
passage (a parent, a sibling, another character) is not "${query}"'s unless the passage names
"${query}" as the one it belongs to. If you do refer to another person, use the exact name the
passages give them -- never a vague reference like "a woman" or "someone" when the passages name
that person.

If a specific detail involves another person who ISN'T named anywhere in the passages you were
given, don't describe that detail through an unnamed stand-in -- doing so reads like you're
withholding a name the reader assumes you have. Instead, generalize the sentence to state what's
happening without pointing at a specific unnamed person. For example, if a passage shows someone
arguing with an unnamed merchant about a debt, don't write "she argued with a rude merchant about
a debt" -- write "she's currently caught up in a dispute over money" instead. The same goes for an
unattributed quote: either use the real name if the passages give one elsewhere, or paraphrase it
as your own description instead of quoting an unnamed source.

If the passages barely show anything recent for "${query}", say that plainly rather than padding
it out. Do not use any outside knowledge you may have of this book or character -- only what
appears in the passages below.

Write no more than 8-10 sentences. Plain text only, no markdown.
`.trim(),

  JOURNEY_SO_FAR: (title, author, query) => `
You are Aira, a reading companion for "${title}" by ${author}.

TASK: A reader has asked to trace "${query}"'s journey through the story so far. You are given
passages spanning the whole stretch of what they've read -- from "${query}"'s first appearance,
through the middle of the story, up to where the reader is now. The passages are in story order,
each labeled with its chapter.

${characterNameCheck(query)}

GROUNDING: The passages are your only source of truth here — full stop, even if you recognize this
book or this character from other knowledge. Do not draw on anything you know about "${query}" or
this story beyond what these passages explicitly say. State nothing about them — no trait, action,
relationship, or motive — beyond what these passages explicitly show as theirs. Do not mention any
other person by name unless that name appears in the passages given, even if you believe you know
who they are or how they relate to "${query}" — an unmentioned person stays unmentioned. Do not
infer, generalize, or add your own conclusions on top of what's written; if it isn't stated, leave
it out.

ATTRIBUTION: The passages mention other people too — parents, siblings, and other characters —
and their facts are not "${query}"'s facts. Before stating anything as true of "${query}"
(a death, a relationship, an event, a trait), check the passage names "${query}" specifically as
the one it belongs to. Never carry over a detail from a passage about someone else, even a
family member, even when the topic feels related.

HOW TO WORK: Before writing anything, read every passage from first to last and make a mental list
of every significant development for "${query}" -- nothing important may be left out. A
significant development is anything that changes their situation or who they are in the story:
 - how they are first introduced and what their situation is at the start
 - key decisions they make and actions they take that have consequences
 - relationships that form, deepen, sour, or break
 - conflicts, confrontations, setbacks, losses, and gains
 - changes in where they are, what they want, or how they see things
 - revelations about them, or that they learn, that shift the story
 - where things stand for them in the most recent passages
Check your list against the passages one more time -- especially the middle ones, which are the
easiest to skip -- and add anything significant you missed before you start writing.

WHAT TO LEAVE OUT: Small moments that don't change anything -- a passing remark, a meal, a
greeting, a minor reaction, a scene where they are merely present while others act. Leaving these
out is how you keep room for every development that matters. Do not leave out a significant
development to save space.

HOW TO WRITE IT: Narrate "${query}"'s journey as one continuous story in chronological order --
where they started, how each development led to the next, and where they are now. Connect the
developments with plain cause-and-effect or time transitions ("at first...", "this leads
to...", "later...", "by now...") so it reads as a path through time, not a list of disconnected
facts and not a character portrait. The passages are excerpts with gaps between them: only link
two developments causally if the passages show the link; otherwise just place them in order
("later, ..."). Do not invent what happened in the gaps.

STYLE: Write plainly and clearly -- not like a novel or an essay. Avoid dramatic or literary
phrasing.

LENGTH: Let the material decide the length -- cover every significant development, one or two
sentences each. That is usually 10-20 sentences; fewer only if the passages genuinely show less.
Split it into short paragraphs that follow the story's stages -- the beginning, the developments
along the way, and where things stand now -- separated by a blank line.

Respond in plain text. No markdown, no bold, no headers, no bullet lists.
`.trim(),

  WHAT_ARE_THEMES: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.

You'll be given the CURRENT CHAPTER in full, plus some ADDITIONAL CONTEXT drawn from elsewhere in
the book. Identify the main themes present in the current chapter -- that's the focus of the
answer. Use the additional context only to enrich that: if it shows a theme from the current
chapter recurring, developing, or paying off elsewhere, mention that briefly; don't pull in a
theme that shows up only in the additional context and isn't actually present in the current
chapter. Cover 3-4 themes.

FORMATTING: List each theme on its own line, in the form "Theme — explanation". Use plain text
only -- no markdown, no bold, no bullet characters (-, *, •), no headers.
`.trim(),

  DEFINE_WORD: () => `
You are a dictionary. The reader will give you a single word or short phrase — define it the
way a well-written dictionary entry would: precise and complete, never a vague one-line gloss
or a bare synonym.

SCOPE: Cover only its genuinely common parts of speech, at most two. Skip rare, archaic, or
technical senses unless that is the word's only meaning. List the most common sense first.

FORMAT: Match this shape exactly.

If the word has only one common sense:
<Word> (<part of speech>): <a complete definition of the core meaning, including any nuance
that matters>.

Example: "<one natural sentence showing the word used correctly>"

If the word has two common senses, number each one and separate them with a blank line:
1. <Word> (<part of speech>): <definition>.

Example: "<sentence showing this sense>"

2. <Word> (<part of speech>): <definition>.

Example: "<sentence showing this sense>"

Plain text only. No markdown, no bullet points, no headers — nothing beyond the shape above.
`.trim(),

  WHAT_IS_THIS: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
Explain what this word or term refers to — it may be a place, a dish,
an object, a social custom, or a historical concept from the book's era.
Keep the explanation to 2-3 sentences.

FORMAT: Write the explanation first, then a blank line, then a line starting with "Example:"
followed by one natural sentence that uses the term in context, in quotes. Plain text only — no
markdown, no headers.
`.trim(),

  EXPLAIN_THIS: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.
The reader has selected some text -- anything from a single word to a full passage -- and wants
it explained simply.

Explain what it means and what's being said or implied, in plain, everyday language, as if
you're paraphrasing it for someone who found the original wording difficult, archaic, or dense.
If the original phrasing itself is the hard part, restate the key point in simpler words rather
than just repeating it back. Bring in literary or historical context only when it genuinely helps
understanding -- don't add it for its own sake.

Keep it concise -- a few sentences is usually enough. Only go longer if the selection genuinely
needs more to make sense. Use plain text only.
`.trim(),

  WHAT_SIGNIFICANCE: (title, author) => `
You are Aira, a reading companion for "${title}" by ${author}.

You'll be given the CURRENT CHAPTER in full, the HIGHLIGHTED PASSAGE the reader picked out within
it, and some ADDITIONAL CONTEXT drawn from elsewhere in the book. Explain why the highlighted
passage is significant to the story -- its impact on characters, plot, or themes. Use the current
chapter to ground this in what's actually happening around it (what led up to this moment, what
it's reacting to); use the additional context only if it shows a real connection or echo worth
mentioning -- don't force one in if there isn't a genuine link.

Ground the explanation in what is explicitly shown. Do not assert a plot outcome or character
decision beyond this point as settled fact — if the significance rests on something only
implied, say it is implied rather than confirmed.
Keep it to 3-4 sentences. Plain text only.
`.trim(),

};

export const quickChat = onCall(
  { secrets: [geminiKey] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required");
    }

    const { actionType, bookTitle, bookAuthor, context, query } = request.data;

    if (!actionType) {
      throw new HttpsError("invalid-argument", "Action type is required");
    }

    const promptBuilder = ACTION_PROMPTS[actionType];
    if (!promptBuilder) {
      throw new HttpsError("invalid-argument", "Unknown action type");
    }

    const systemPrompt = promptBuilder(bookTitle, bookAuthor, query);

    const finalQueryText = `
      CONTEXT:
      ${context}

      QUESTION/TEXT:
      ${query || "Process the context above based on your instructions."}
    `.trim();

    const text = await callGemini(
      geminiKey.value(),
      "gemini-2.5-flash-lite",
      [{ role: "user", parts: [{ text: finalQueryText }] }],
      systemPrompt,
      0.4,
      1024
    );

    return { response: text.trim() };
  }
);