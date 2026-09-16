# Aira Response Quality — Working Notes

Session log of diagnosis and fixes for Aira's response quality across the peek-bar quick actions
and the full chat screen. Written as a handoff/reference doc, not a spec — captures what was
actually found, what was changed, and what's still open.

## Background: the two surfaces

- **Peek bar (`AiraPeekBar`)** — tap-triggered quick actions (Define, Who is this?, What is this?,
  Summarize chapter, etc.). Deterministic input (the exact selected text/word, or a known chapter
  index) — no classification needed. Stateless: no chat history, no persistence.
  Code: `aira/.../chat/quickChat/*`, `functions/src/ai/chat/quickChat.ts`.
- **Chat screen (`AiraChatScreen`)** — free-typed questions. Routed through `detectIntent.ts`
  (classifies `isRAG` + `queryIntent` + extracts `entities`/`keywords`/`queryVariations`), then to
  either `bookChat.ts` (RAG-grounded) or `generalChat.ts` (general knowledge). Carries conversation
  history; every turn is persisted to `AiraMessageDao`.
  Code: `aira/.../chat/bookChat/*`, `aira/.../chat/generalChat/*`, `aira/.../intent/*`,
  `aira/.../service/AiraChatService.kt`.

**Decision, confirmed with the user:** these two surfaces stay architecturally separate.
Quick-action prompts are not reused/merged into chat, and chat's typed-question history is not
extended to quick actions. Quick actions were already working well; the chat surface was the
one with real problems.

## Issue 1 — Define quick action: shallow definitions

`DEFINE_WORD` in `quickChat.ts` gave a terse, inconsistent definition with an optional example
("if helpful") and no guaranteed part-of-speech callout.

**Fix (shipped):** rewrote the prompt to always state part of speech (covering multiple senses if
applicable), give a fuller definition per sense, and always include an example sentence per sense.
Dropped the old hard "2-3 sentences" cap. Plain-text-only formatting kept.

File: `functions/src/ai/chat/quickChat.ts` (`DEFINE_WORD` prompt).

## Issue 2 — General chat rambles on simple questions

Example: "What does fidelity mean?" in the chat screen produced a multi-paragraph thematic essay
tying the word back to the novel, instead of a definition.

**Root cause:** `generalChat.ts`'s system prompt has no length discipline (`bookChat.ts` caps at
"80 words or less"; `generalChat.ts` only has a vague "be concise") and no branch for direct
factual/definitional questions vs. open discussion — every question gets the same "chat naturally,
weave in themes" treatment. `quickChat.ts`'s `CUSTOM_QUESTION` prompt already has the right
instinct for this ("if it's about a word/phrase, answer directly and concisely") but that logic was
never ported to `generalChat.ts`/`bookChat.ts`.

**Proposed fix (discussed, NOT implemented yet):** add a `responseMode: "direct" | "discussion"`
field to `detectIntent.ts`'s classifier output (decided in the same router call, no extra latency),
thread it through `Intent.General`/`Intent.BookRelated`, and give `bookChat.ts`/`generalChat.ts` a
conditional prompt branch — `direct` answers the question plainly in 1-4 sentences with no forced
book tie-in; `discussion` keeps today's conversational behavior. This is still open — pick it up
whenever the chat's answer *tone* (not retrieval) needs work again.

## Issue 3 — "Who is X" answers differently in chat vs. the peek bar

Diagnosed as three compounding differences, not one:

1. **Prompt posture.** Peek bar's `WHO_IS_THIS` is a grounded dossier lookup ("based ONLY on the
   passages... do NOT reveal anything beyond what they contain... 3-4 sentences"). `bookChat.ts`'s
   prompt is written to sound like "a warm, passionate friend... stitch together partial
   information" — structurally different license to embellish.
2. **Retrieval focus** (see Issue 4).
3. **History.** `bookChat.ts` includes prior chat turns in the Gemini call; `quickChat.ts` never
   does. Chat answers can be colored by whatever was discussed right before.

## Issue 4 — Chat's RAG retrieval was diffuse for "who is X" / relationship questions

**Peek bar's `WhoIsThis`** (`QuickChatRepositoryImpl.kt`) embeds one precise, human-authored query
(`"$name character person description role"`) and takes the top 3 passages via `RagRetriever.retrieve()`.

**Chat's default path** (`BookChatRepositoryImpl.kt`, pre-fix) always called
`retrieveWithExpansion()` with the router's 3 LLM-paraphrased `queryVariations`, regardless of
`queryIntent` — wider net, less centered on "describe this one character."

### What shipped

`BookChatRepositoryImpl.kt` now branches on `route.intent`:

- **`CHARACTER_INFO`** with resolved entities → one focused `retrieveWithExpansion()` call *per
  entity*, with `queries = listOf("$entity character person description role")` (single precise
  query, not diluted across paraphrases), `entities`/`boostedKeywords` still set so FTS lexical
  anchoring still applies, `topPassages = 3`. Results merged/deduped across entities. This mirrors
  `WhoIsThis`'s query-construction approach.
- **Everything else** (including `RELATIONSHIP`) → unchanged `retrieveWithExpansion(queries =
  route.queryVariations, ...)`.

### The `RELATIONSHIP` regression (and why it was reverted)

First attempt added a similar focused branch for `RELATIONSHIP` — one *combined* query
(`"A and B relationship"`) instead of per-entity queries, reasoning that a relationship question
needs passages where both people co-occur. **This made answers measurably worse** (verified via
logcat): the router's own 3 `queryVariations` for `relationship`-intent questions were already
well-targeted, entity-anchored phrasings (e.g. "the relationship between X and Y", "the dynamic
between X and Y", "the interactions between X and Y") — not vague generic paraphrases. Collapsing
that to a single query lost real semantic coverage for content that's scattered across multiple,
differently-worded passages (a multi-scene confrontation, not a single concentrated bio). Reverted;
`RELATIONSHIP` now falls through to the original unmodified `retrieveWithExpansion` path.

**Lesson recorded here so it isn't repeated:** "laser focus" only helps when the router's own
query variations are genuinely diffuse. For `CHARACTER_INFO` (bio-style, concentrated in one
place in the text) a single precise query beats 3 paraphrases. For `RELATIONSHIP` (content
scattered across multiple scenes) the router's multi-query redundancy was already doing real work
— don't collapse it without evidence from logs first.

### Multi-entity design (discussed, not yet built)

- Two people, "who are X and Y" style (still `CHARACTER_INFO`) → keep doing separate focused
  retrieval per entity and merge (already how the `CHARACTER_INFO` branch works for multiple
  entities).
- Two people, relationship-style ("how is X related to Y") → combined query naming both, not
  per-entity — this is the `RELATIONSHIP` case, currently on the default/unfocused path per the
  revert above.

Files: `aira/.../chat/bookChat/repository/BookChatRepositoryImpl.kt`.

## Issue 5 — `detectIntent` hallucinated a character name (cross-book contamination)

Question: *"what happened between mitya and the colonel katya's father"* (Katerina Ivanovna's
father is unnamed/rarely named in *The Brothers Karamazov*).

**Logcat showed the router invented:** `"Colonel Yegor Ilyich Svidrigailov"` — **Svidrigailov is a
character from Dostoevsky's *Crime and Punishment*, a different novel by the same author.** The
classifier (gemini-2.5-flash-lite, working from the question text alone, no book content) crossed
its own training knowledge between two books.

This poisoned every downstream field: all 3 `queryVariations` referenced the fake name, `entities`
included it (feeding FTS anchoring with a term that will never match this book's text), retrieval
came back with a noisy/unrelated mix, and `bookChat.ts`'s "synthesize the most complete answer
possible" instruction turned that noise into a confident-sounding but fabricated narrative
(wrong facts about the colonel's interactions with Mitya, an uninvolved "colonel's daughter"
claim, etc).

**Fix (shipped):** added an explicit `GROUNDING RULE` block to `detectIntent.ts`'s router prompt —
only use a name if certain it belongs to *this specific book*; never borrow a name from a
different book, even by the same author; if a person is referred to by role/relation ("Katya's
father", "the colonel") and the real name isn't known with certainty, keep the description as-is
rather than fabricate a name. Applies to `entities`, `queryVariations`, and `keywords` alike.

File: `functions/src/ai/intent/detectIntent.ts`.

## Issue 6 — Intent-aware `bookChat.ts` prompt branching

Even with correct retrieval, `bookChat.ts`'s single instruction ("explain events... stitch together
partial information") biased every answer toward narrating one concrete scene, even for questions
that wanted a character read (a relationship's *nature*, not a blow-by-blow retelling). Example:
asked about the Grushenka/Fyodor dynamic, got a play-by-play of one specific scene instead of a
characterization of the obsession/power dynamic.

**Fix (shipped):** `route.intent` (from `Intent.BookRelated`) now flows through
`BookChatPayload.queryIntent` into `bookChat.ts`, which picks an intent-specific instruction line:

- `character_info` → describe who they are (personality/role/traits), don't just narrate a scene.
- `relationship` → characterize the dynamic (mutual/one-sided, affectionate/manipulative,
  evolving/static), don't just retell one incident.
- `plot_event` → narrate events clearly, in order.
- `quote_lookup` → quote/paraphrase precisely rather than summarizing loosely.
- `general`/unrecognized → falls back to the original "explain events, stitch together" wording
  (no regression for unmapped or missing values).

Files: `aira/.../chat/bookChat/payload/BookChatPayload.kt`,
`aira/.../chat/bookChat/repository/BookChatRepositoryImpl.kt`, `functions/src/ai/chat/bookChat.ts`.

## Still open

- **`responseMode` (direct/discussion) split** for `generalChat.ts`/`bookChat.ts` — see Issue 2.
  Not implemented.
- **"Katya's older sister" retrieval miss** — a real character (named when Mitya recounts meeting
  Katerina to Alyosha) that chat's retrieval failed to surface, unlike the Svidrigailov case this
  is a genuine content-not-found miss, not a hallucination. Two live theories, unconfirmed:
  (a) the router couldn't resolve/name her from the question alone, so `queryVariations` only had
  a vague "Katya's older sister" phrasing to search with; (b) the passage may sit beyond the
  reader's spoiler-lock chapter ceiling and is correctly excluded from the candidate pool. Needs
  logcat (`IntentDetection` + `BookChatRepo` tags) for that exact question to disambiguate before
  making any change. *Partially related to Issue 7 below (theory (a) is a semantic-anchor-score
  miss, same failure class as the Trifonov bug) — worth re-checking against the Session 2 fix
  before assuming it's still broken.*
- **Vestigial JSON-parsing block in `BookChatRepositoryImpl.kt`** (lines ~83-106 at time of
  writing): tries to parse the Gemini response as `{"answer": "..."}`, but `bookChat.ts` only ever
  returns plain prose. The parse always throws and falls through to the raw-text fallback, so it's
  currently harmless but dead work on every call — worth deleting next time this file is touched.
- **Deployment:** all `.ts` changes (`quickChat.ts`, `bookChat.ts`, `detectIntent.ts`) require
  `firebase deploy --only functions:<name>` to take effect — not run automatically as part of
  this work.

## Files touched — Session 1

- `functions/src/ai/chat/quickChat.ts` — `DEFINE_WORD` prompt rewrite.
- `functions/src/ai/chat/bookChat.ts` — intent-aware prompt branching.
- `functions/src/ai/intent/detectIntent.ts` — grounding rule against name hallucination.
- `aira/src/main/java/com/yugentech/quill/aira/chat/bookChat/repository/BookChatRepositoryImpl.kt`
  — focused retrieval for `CHARACTER_INFO`; `queryIntent` passed into the payload.
- `aira/src/main/java/com/yugentech/quill/aira/chat/bookChat/payload/BookChatPayload.kt` — added
  `queryIntent` field.

---

# Session 2 — 2026-09-16

Picks up mid-branch (`fix/summarize-chapter-chunk-overlap`). Two pieces of uncommitted work already
existed on the branch at session start, unrelated to Session 1's doc above:

- **Chunk-overlap fix for `SUMMARIZE_CHAPTER`** — `EpubTextExtractor.extractChapter()` now reads a
  chapter's plain text directly from the EPUB spine instead of joining overlapping RAG chunks
  (which duplicated text at chunk boundaries when naively concatenated).
  `QuickChatRepositoryImpl`'s `SummarizeChapter` branch and `AiraModule`'s DI wiring were updated
  to match.
- **Anti-misattribution grounding rules added across several `quickChat.ts` prompts**
  (`SUMMARIZE_CHAPTER`, `WHO_ARE_CHARACTERS`, `WHO_IS_THIS` at the time, `DEFINE_WORD`,
  `WHAT_IS_SIGNIFICANCE`) — "don't invent/misattribute an action, say when something is only
  implied." Likely a follow-on fix once the chunk-overlap fix above stopped feeding the model
  duplicated boundary text that had been causing confident wrong attributions.

Also present on the branch, unrelated to Aira quality at all: query-limit enforcement UI changes in
`AiraChatScreen.kt`/`AiraPeekBar.kt` (moved the `canSendQuery` check to fire on send/chip-tap
instead of a `LaunchedEffect`).

## Issue 7 — `WhoIsThis` missed a character whose name was on screen (sparse-mention recall failure)

Reported by user: selected "Trifonov" (a real, named minor character — a merchant mentioned in an
anecdote about a lieutenant-colonel) and tapped "Who is this?". Aira responded "The provided
passages do not mention a character named Trifonov" — an LLM-generated refusal, meaning retrieval
returned *some* passages (not empty, or the client's own hardcoded "I haven't encountered X" error
would have fired instead), just not the right one.

**Root cause:** `RagRetriever.retrieve()` (used by `WhoIsThis` and every other peek-bar quick
action) was pure vector/embedding search with a hard `ANCHOR_MIN_SCORE` (0.40) cosine-similarity
cutoff — any candidate scoring below that is dropped, with no lexical/FTS anchoring at all (unlike
`retrieveWithExpansion()`, used by chat's `CHARACTER_INFO` path, which does resolve FTS positions
for the entity name). Trifonov is a minor character named in a paragraph that's mostly *about* the
lieutenant-colonel — the chunk's embedding for a generic "Trifonov character person description
role" query likely undershot the 0.40 anchor, so it got dropped even though the literal name was
right there, and some other, wrong passage(s) got sent to the LLM instead.

**Fix (shipped):** in `RagRetriever.kt`, both `retrieve()` and `retrieveWithExpansion()` now: when
an entity name is given, first find its FTS-confirmed literal matches within the spoiler-locked
pool; if any exist, narrow the candidate pool to just those matches and rank them by semantic
similarity with **no absolute score floor** (a literal name match is already sufficient evidence of
relevance — it shouldn't also have to win a similarity contest it was never going to win). If no
FTS matches exist, falls back unchanged to the original full-pool + `ANCHOR_MIN_SCORE` behavior.
`retrieve()` gained `entities`/`boostedKeywords` params to support this; `QuickChatRepositoryImpl`'s
`WhoIsThis` call now passes `entities = listOf(quickPrompt.name)`.

Considered and rejected during design: an earlier version scored *all* candidates (FTS match OR
above-threshold) as a union rather than narrowing the pool — rejected because for a well-known
character with hundreds of literal mentions, that reopens the door to unrelated chunks about *other*
characters that happen to score decently against generic "character person description role"
phrasing, undoing the point of narrowing in the first place. Narrowing-then-ranking (no union) keeps
faith with the same focused-retrieval philosophy already validated for chat's `CHARACTER_INFO` path
(Issue 4/Session 1) — for a common character the narrow pool is barely narrower than the full pool
(their name is everywhere), so nothing changes for them; for a rare one it's the difference between
finding the one existing mention and never seeing it.

Files: `aira/src/main/java/com/yugentech/quill/aira/rag/RagRetriever.kt`,
`aira/src/main/java/com/yugentech/quill/aira/chat/quickChat/repository/QuickChatRepositoryImpl.kt`.

## Issue 8 — `WhoIsThis` answers read as a disconnected fact-list, with ungrounded editorializing

Reported by user, two examples on the same book (*The Brothers Karamazov*): asking about Mitya
produced "Mitya... is a man who is deeply distressed... He is concerned about a debt... Mitya has a
complex relationship with Grushenka... He is also the brother of Alyosha" — one fact per sentence,
no connective thread. Asking about Katya produced a similar list that ended with the model implying
her dowry makes her "desirable" — a conclusion the text itself never states.

**Root cause:** the old `WHO_IS_THIS` prompt told the model *what* to cover (role, personality,
relationships) with a "3-4 sentences" cap, but never told it to *connect* those facts — with several
independent facts and a tight sentence budget, one-fact-per-sentence is the safe default. Nothing
told it to stop at reporting facts rather than appending an inferred conclusion.

**Fix (shipped, rewritten twice — see below):** rewrote `WHO_IS_THIS` around two general principles
instead of a growing list of specific patched-on rules:
- **GROUNDING** — state nothing about the person beyond what the passages explicitly show as
  theirs; no inference, no added conclusions.
- **SYNTHESIS** — read all the passages first, form one coherent view, express it as a single
  flowing sketch where each sentence builds on the last, not facts listed independently.

Design note: the first attempt at this fix wrote GROUNDING with a baked-in specific example (the
"dowry → desirable" case) and a separately-called-out misattribution rule. User corrected this —
both are just instances of the same general grounding failure (stating something the text didn't
say), so calling them out individually invites an ever-growing patch list every time a new failure
shape shows up. The two-principle version above is deliberately general so new failures should be
*covered* by GROUNDING/SYNTHESIS rather than needing another bullet appended.

Verified against real output: the same underlying facts (Alyosha's modesty, class rank, indifference
to money, being sent by Zossima) produced a noticeably better, more connected answer under this
prompt than under the original — user confirmed and had it reapplied after a brief revert-to-original
comparison.

File: `functions/src/ai/chat/quickChat.ts` (`WHO_IS_THIS` prompt).

## Issue 9 — `DEFINE_WORD` answers lacked structure

**Fix (shipped):** rewrote around an explicit template instead of loose prose instructions —
**SCOPE** (cover at most the 2 genuinely common parts of speech, skip rare/archaic senses) and
**FORMAT** (one exact line per sense: `<part of speech>: <definition> — "<example sentence>"`, most
common sense first). Gives the model a concrete shape to fill rather than leaving layout to chance.

File: `functions/src/ai/chat/quickChat.ts` (`DEFINE_WORD` prompt).

## Issue 10 — Spoiler lock is ignored by every RAG-using peek-bar quick action (found, not fixed)

`BookChatRepositoryImpl.kt` (chat screen) correctly passes `spoilerLockEnabled = book.spoilerLockEnabled`
— it respects the reader's per-book toggle (`AiraViewModel.toggleSpoilerLock()`, persisted via
`bookRepository.setSpoilerLock`, defaults to `true`). `QuickChatRepositoryImpl.kt` hardcodes
`spoilerLockEnabled = true` on all six of its `ragRetriever.retrieve()` calls
(`WhoAreTheCharacters`, `WhoIsThis`, `WhatAreTheThemes`, `WhatIsTheSignificance`, `WhoIsSpeaking`,
`CustomQuestion`), ignoring the setting entirely. Practical effect: a reader who turns spoiler lock
off (e.g. a reread) still gets every peek-bar quick action silently clamped to their reading
progress, while the chat screen correctly uses the whole book. Not fixed yet — flagged for a future
session.

## Issue 11 — `WhoIsThis` quality is inconsistent across reading progress, for prominent characters (diagnosed, not fixed)

Reported by user: asked "who is Dmitri" at four different points while reading *The Brothers
Karamazov* (chapter 4, book 8, book 9, book 10). Richness did not grow monotonically with progress
the way a human would expect — some answers correctly surfaced his inheritance dispute and army
background, others dropped that entirely in favor of only the most recent scene (the Grushenka kiss,
the trial), and "queen of my soul" appeared as a bare epithet never tied back to Grushenka.

**Root cause (not a prompt problem — this is retrieval budget vs. a growing pool):** `WhoIsThis` is
deliberately stateless (no history, no persistence — see Background section above), so every tap is
a cold retrieval: same fixed generic query, same `topPassages = 3` (+ neighbor window), against
whatever's unlocked so far. Early on, the candidate pool is small and the character's one dedicated
introduction chunk dominates the top 3. By book 9-10, the pool has grown to dozens of scenes, but the
budget didn't grow with it — the top-3 slots become a lottery increasingly won by whichever chunks
are most recent/emotionally vivid, edging out the (still-present, still-relevant) introduction
chunk. Nothing is being "forgotten" — the introduction chunk is just losing the auction as
competition increases. Separately, there's no alias/epithet resolution anywhere in the pipeline, so
a passage referring to Grushenka only as "queen of my soul" never gets tied to the entity "Grushenka"
established elsewhere.

Diagnosis process ruled out (with the user) several heavier fixes before landing on the current
design direction:
- **Persistent per-character memory** (an incrementally-updated cached profile per character) —
  rejected. Only grows if a character is asked about repeatedly; no guarantee of that; real upkeep
  cost for uncertain payoff; would make quality depend on *how often* a character is queried, which
  is explicitly the invariant we don't want (see design principle below).
- **Agentic tool-calling loop** (LLM decides mid-generation it needs another fetch, e.g. via
  LangChain/LangGraph or native Gemini function calling) — technically feasible (Gemini's function
  calling doesn't require the tool executor to be in the same process; the Android app could execute
  each requested fetch locally and re-invoke the Cloud Function with the result, treating each round
  as one more stateless network call) but rejected for now: retrieval lives on-device
  (`RagRetriever`/Room), the LLM call is server-side, so each agent "tool call" round costs a full
  phone↔cloud↔Gemini↔phone round trip — multiple rounds means several seconds of latency for what's
  supposed to be an instant quick action, plus real new client-side dispatch-loop complexity.
- **Any Tier-2-style import-time LLM/NER preprocessing** (alias/entity graphs, GraphRAG, RAPTOR
  summaries, LLM-generated contextual chunk blurbs à la Anthropic's "contextual retrieval") —
  dropped entirely. Would require a new network dependency at book-import time (today's indexing,
  `BookEmbeddingWorker`/`EmbeddingEngine`, is fully offline/on-device) for uncertain per-book payoff.
- **Embedding chapter/book metadata into the vector itself** (e.g. prepending `chapterTitle` before
  calling `embeddingEngine.embed(...)`) — considered, rejected. `BookChunkEntity` already stores
  `chapterIndex`/`chapterTitle`/`chunkIndex` correctly for every chunk (verified — nothing missing
  from indexing); the gap is only that `BookEmbeddingWorker.kt:73` embeds `chunk.text` alone, with no
  structural signal. Folding metadata into the vector was rejected because its effect on similarity
  scores is diffuse and hard to reason about/tune (risk of a shared chapter-title prefix pulling
  same-chapter chunks artificially closer together, or biasing on the title's literal words), and
  there's no eval harness to measure whether it actually helped. Decided instead to keep embeddings
  pure (content-only) and use the already-stored metadata as explicit, inspectable logic in the
  retrieval ladder below — easier to reason about and adjust one rule at a time.

**Design principle locked in from this discussion:** answer quality must be consistent regardless of
how many times a character has been asked about — 1st ask or 10th, same deterministic single-shot
retrieval + one synthesis call, nothing persisted or "remembered" between calls. The LLM is used only
for final synthesis, never for retrieval orchestration.

### Tier 1 ladder design for `WhoIsThis` (in progress — this is the active design thread)

Priority order for peek-bar quick actions going forward: `DefineWord` and `SummarizeChapter` don't
use RAG and are considered solved; `WhoIsThis` is the highest-leverage remaining problem (most
reader questions are character-related, and cracking single-character retrieval generalizes to
relationship/theme questions later).

**Locked:**
- **Step 1 — Count FTS occurrences of the queried name** within the spoiler-locked candidate pool
  *before* any semantic ranking. This count decides the regime:
  - **Sparse** (a handful of hits, e.g. Trifonov) → take every matched chunk directly, no ranking
    needed, nothing to triage.
  - **High** (dozens+, e.g. Dmitri/Alyosha) → needs Step 2's selective triage.
- **Step 2 (high-count regime only) — Split the matched pool into three zones by position**,
  modeled on how a human would research a character with a searchable book in hand (search
  everything, then triage by information density — don't read every mention equally):
  - **Recent** — pinpointed directly near `book.lastChapterIndex` (the reader's overall furthest
    progress, not the exact tapped-text location). Cheap and precise: we already know exactly where
    "now" is, no search needed.
  - **Early** — no fixed anchor exists for "the introduction," so this needs the wider semantic+FTS
    ranking machinery. Gets the most retrieval effort of the three zones, since it's the one zone we
    can't pinpoint.
  - **Middle** — light scatter, fills whatever budget remains, lower priority than the other two.

**Shelved (not building yet — revisit only if Steps 1-2 alone prove insufficient):**
- **Step 3 — Relationships.** Detect names that recur *within the chunk set already retrieved for
  the queried character* (a scoped, per-query heuristic — count recurring capitalized tokens in that
  small already-fetched set — explicitly not a pre-built global entity graph, which would need
  NER/LLM and was already ruled out). Cap to the top 1-2 most frequent co-occurring names; run a
  lightweight fetch for each (not the full ladder — they're supporting context, not the subject);
  merge into the final context. Key risk flagged: the synthesis prompt must frame these chunks
  strictly as "context for the relationship," not independent characterization of the other person,
  or it reintroduces the misattribution problem Issue 8's GROUNDING principle was built to prevent.

**Considered and explicitly rejected as additional factors:**
- **Chapter-title name boost** (jump the queue for chunks whose `chapterTitle` contains the queried
  name) — rejected: breaks the primacy/recency/middle chronological philosophy of Step 2, and EPUB
  formatting inconsistently splits chapter/book titles into separate spine items, so it would
  frequently just waste a lookup.
- **Breadth (distinct chapters mentioning the character) vs. raw mention count** — unnecessary:
  Step 1's count only decides the regime; Step 2's position-based zone selection already prevents
  one chatty scene from dominating, since zones are chosen by position, not frequency.
  - **Evidence-type distinction** (narrator exposition vs. another character's dialogue about them
  vs. their own action/dialogue) — left to the LLM at synthesis time; more information for the
  model to work with, not a retrieval-level concern.
- **Turning-point/significance weighting** (prioritize the scene where something decisive happens to
  the character) — correctly identified as the most human-like signal, but not buildable without
  per-passage LLM judgment, which is out of scope for the current no-LLM-indexing design.

**Open, unresolved gap (flagged, not solved):** **same-character name variants** — e.g. "Mitya" vs.
"Dmitri" vs. "Dmitri Fyodorovitch" vs. "Mityenka," common in translated 19th-century Russian
literature specifically. FTS only matches the literal tapped string, so if the early-zone
introduction predominantly uses a different name form than the one the reader tapped, Step 1
undercounts and Step 2's early zone can come back empty even for a genuinely well-documented
character — hitting hardest exactly where Step 2's sophistication matters most. No clean fix
without LLM/coreference work, which conflicts with the no-LLM-indexing decision above. Tentatively
accepted as a known limitation: the reader's tapped form is always guaranteed to be found (it's
literally on their current page), only the *other* zones risk incompleteness.

**Still undecided, deferred to a future session:**
- Exact width of the "recent" zone (just the current chapter, or a small range before
  `lastChapterIndex`?).
- Budget split across recent/early/middle zones.
- Whether to drop the existing `PASSAGE_WINDOW_BEFORE/AFTER` (±1) neighbor-chunk expansion for
  `WhoIsThis` specifically. Leaning toward dropping it in favor of more *distinct* anchors — a
  neighbor chunk is often not about the queried character at all (just adjacent narrative content),
  which both wastes budget and reintroduces misattribution risk; `ChunkingStrategy.kt` already snaps
  chunk boundaries to sentence endings, which mitigates most of the original reason neighbors were
  wanted (avoiding mid-sentence truncation). Not finalized.
- Target total chunk count for the high-count regime. Reconsidered mid-discussion: token budget is
  **not** the real constraint — Gemini 2.5 Flash-Lite supports 1,048,576 input tokens / 65,535 output
  tokens, and current `WhoIsThis` usage is only ~2,300-3,800 tokens/call (under 0.5% of capacity), so
  cost/latency impact of going to 15-20 chunks is negligible. The real constraints are (a) chunk
  *diversity* — more chunks only help if they're genuinely distinct facets, not near-duplicate
  restatements of whatever's currently loudest (this is what Step 2's zones are for) and (b) that the
  answer is currently a fixed 3-4 sentences regardless of how much is retrieved, which wastes a richer
  retrieval on prominent characters. Leaning toward: **answer length should scale with the regime**
  (short for sparse characters, longer — maybe 5-8 sentences — for high-count/well-documented ones)
  so a wider, diversified retrieval actually shows up in the output instead of being silently
  discarded to fit a one-size-fits-all length cap. Not finalized.
- Whether Step 3 (relationships) ends up needed at all, pending whether Steps 1-2 alone are
  sufficient once built and tested.

Files (not yet changed for this ladder — design only so far): would touch
`aira/src/main/java/com/yugentech/quill/aira/rag/RagRetriever.kt` and
`aira/src/main/java/com/yugentech/quill/aira/chat/quickChat/repository/QuickChatRepositoryImpl.kt`.

## Files touched — Session 2

- `aira/src/main/java/com/yugentech/quill/aira/rag/RagRetriever.kt` — FTS-narrowed-pool retrieval
  fix (Issue 7); shared `scoreCandidates` helper with optional score floor.
- `aira/src/main/java/com/yugentech/quill/aira/chat/quickChat/repository/QuickChatRepositoryImpl.kt`
  — `WhoIsThis` now passes `entities = listOf(quickPrompt.name)`.
- `functions/src/ai/chat/quickChat.ts` — `WHO_IS_THIS` prompt rewrite (Issue 8), `DEFINE_WORD`
  prompt rewrite (Issue 9).
- `functions/src/ai/chat/bookChat.ts` — touched then reverted (misdiagnosed Issue 8 as a `bookChat`
  problem before the user corrected it was `quickChat`'s `WHO_IS_THIS`); no net change.

---

# Session 3 — 2026-09-16

Implemented Issue 11's Steps 1–2 (regime count + zone triage), scoped down from the doc's original
"last third of matches" recency idea after user feedback, and built as a dedicated function rather
than another branch inside `retrieve()`/`retrieveWithExpansion()`.

## Issue 11 continued — Tier-1 ladder implemented as `RagRetriever.retrieveForCharacter()`

Two design refinements emerged during this session that go beyond what Session 2 had written down:

1. **Step 1's FTS count must ignore spoiler lock; actual chunk selection never does.** Counting
   matches only within the locked pool would make every character look "sparse" early in the book
   (nothing's been read yet), even one who's actually central to the whole novel — defeating the
   point of a regime split. `resolveFtsPositions()` already queries FTS with no chapter ceiling, so
   the whole-book count was free to obtain; it's used purely to pick SPARSE vs. HIGH-COUNT. Every
   chunk actually sent to the LLM is still filtered through the spoiler-locked candidate pool
   first — nothing is ever spoiled, only the regime decision looks past the reader's progress.
2. **"Recent" is the literal tapped chunk, not a positional heuristic.** `QuickPrompt.WhoIsThis`
   already carried `currentChapterIndex` (passed in by `resolveChips.kt`, previously unused by
   retrieval). Rather than inferring recency from "last third of matched positions," the new code
   finds the actual chunk in that chapter containing the tapped name (literal case-insensitive
   substring match via `getChunksForChapter`) and takes it plus the 5 chunks immediately before it —
   literally "what the reader just read," not an approximation of it. This zone is NOT filtered to
   FTS matches — it's a contiguous window regardless of whether the character's name appears in
   every chunk of it, since surrounding narrative context (pronouns, dialogue) is still relevant.

### Ladder as implemented

- **Step 1:** `wholeBookMatchCount = resolveFtsPositions(bookId, [name]).size` (whole book, no
  spoiler lock). `== 0` → falls back to the same full-pool `ANCHOR_MIN_SCORE` semantic search
  `retrieve()` already uses as its safety net. `<= 6` → SPARSE. Otherwise → HIGH-COUNT.
- **SPARSE:** every FTS-matched position that's also inside the spoiler-locked pool, taken
  directly, no ranking, no neighbor padding.
- **HIGH-COUNT**, three zones merged and deduped:
  - **Recent** (budget 6) — anchor chunk (literal tapped chunk in `currentChapterIndex`) + 5
    chunks before it, clamped at the chapter start (does not reach into the previous chapter).
  - **Early** (budget 8) — remaining locked FTS-matches (after removing anything already in
    Recent), sorted by position, first half, ranked by cosine similarity against
    `"$name character person description role"`.
  - **Middle** (budget 6) — same remaining-matches list's second half, ranked the same way.
  - Total target ~20 chunks for a well-documented character (comfortably over "at least 15").
  - Any zone with fewer candidates than its budget just contributes what it has; no zone borrows
    another's budget. If no anchor chunk is found in `currentChapterIndex` (rare — name doesn't
    appear verbatim there), Recent contributes nothing and the total is simply smaller.

Reused only existing DAO methods — no new queries: `resolveFtsPositions` (whole-book FTS),
`getCandidates` (locked pool + embeddings), `scoreCandidates` (cosine ranking), `embedQuery`,
`getChunksForChapter` (anchor lookup), `getNeighborChunks` (both the Recent zone's contiguous
window, and — called per-position with `from == to` — fetching individual Early/Middle chunks by
position, the same pattern `retrieveAsPassages` already used).

Per this session's earlier discussion (Q4 in planning), the existing ±1 neighbor-chunk padding used
by `retrieve()`/`retrieveWithExpansion()` for scored anchors is deliberately **not** applied to
Early/Middle zone chunks here — each is taken as exactly the one matched/ranked chunk, no padding.

### Call site change

`QuickChatRepositoryImpl.kt`'s `WhoIsThis` branch now calls `ragRetriever.retrieveForCharacter(
bookId, quickPrompt.name, quickPrompt.currentChapterIndex, spoilerLockEnabled = true)` instead of
`ragRetriever.retrieve(...)`. `currentChapterIndex` was already on `QuickPrompt.WhoIsThis` and
already populated by `resolveChips.kt` — no reader/UI plumbing changes were needed. The existing
"I haven't encountered ..." empty-result error path is unchanged.

No other quick action was touched — `WhoAreTheCharacters`, `WhatAreTheThemes`, etc. still use
`retrieve()`/`retrieveWithExpansion()`, and chat's `CHARACTER_INFO` path (`BookChatRepositoryImpl.kt`)
is untouched, per the doc's "surfaces stay architecturally separate" decision (see Background).

### Still open from Issue 11 (not part of this session)

- **Step 3 (relationship co-occurrence)** — still shelved, per Session 2's doc, pending evidence
  that Steps 1–2 alone aren't enough.
- **Same-character name variants** (Mitya/Dmitri/etc.) — still an accepted, unsolved limitation;
  `findAnchorPosition`'s literal substring match inherits this (only finds the exact tapped form).
- **Answer length scaling with regime** — doc floated this (richer retrieval for HIGH-COUNT should
  produce a longer synthesized answer than SPARSE's terse one); not implemented this session. The
  `WHO_IS_THIS` prompt (`functions/src/ai/chat/quickChat.ts`) still has one fixed length target
  regardless of how many chunks were retrieved — worth revisiting once real HIGH-COUNT output is
  reviewed, since ~20 chunks compressed into the same length budget as 2 chunks may under-use the
  richer context.
- **Verification not yet done this session** — build succeeded (`:aira:compileDebugKotlin` and
  `:app:compileDebugKotlin` both pass), but the ladder has not yet been exercised against a real
  book in the running app / checked against logcat for regime/zone counts. Do that before
  considering Issue 11 closed.

Files touched — Session 3:

- `aira/src/main/java/com/yugentech/quill/aira/rag/RagRetriever.kt` — new `retrieveForCharacter()`
  public method plus private helpers (`findAnchorPosition`, `fetchRecentZone`, `rankAndFetch`,
  `fetchChunksAt`); new companion constants (`SPARSE_MATCH_THRESHOLD`, `CHARACTER_RECENT_LOOKBACK`,
  `CHARACTER_EARLY_BUDGET`, `CHARACTER_MIDDLE_BUDGET`, `CHARACTER_UNRANKED_SCORE`).
- `aira/src/main/java/com/yugentech/quill/aira/chat/quickChat/repository/QuickChatRepositoryImpl.kt`
  — `WhoIsThis` branch calls `retrieveForCharacter()` instead of `retrieve()`.

## Issue 12 — `WHO_IS_THIS` responses read as overly literary/flowery

User feedback after using the new retrieval: answers felt "weird" — narrowed down (via sample
comparison across four style mockups: flowing-synthesis/dossier/conversational/direct-answer) to
one concrete complaint: **overly literary phrasing** (things like "a man of extremes", "consuming
passion", "all-or-nothing intensity"). User couldn't commit to one of the four sample styles as a
full replacement, so the fix targets the diagnosed problem directly rather than adopting a
different style wholesale.

**Fix (shipped):** `WHO_IS_THIS` prompt (`functions/src/ai/chat/quickChat.ts`) gets a new STYLE
block — explicit "write like describing someone to a friend, not a novel" instruction with banned
example phrasing pulled from the actual flowery sample that was shown and implicitly rejected.
GROUNDING and SYNTHESIS (Issue 8) are kept as-is — this is additive, not a reversion, since the
original one-fact-per-sentence problem SYNTHESIS fixed is a separate failure mode from floweriness
and reintroducing it would trade one problem for another.

Also replaced the fixed "3-4 sentences" cap with a LENGTH instruction that scales with how much
the passages actually say (1-2 sentences if sparse, more if there's real material) — ties into
Issue 11's now-variable retrieval size (SPARSE regime ~2-6 chunks vs HIGH-COUNT ~20), which a fixed
sentence cap would otherwise waste.

### Regression from the uncapped LENGTH wording, and the follow-up fix

User tested against a HIGH-COUNT character (Alyosha, ~20 retrieved passages spanning very
different scenes across the book) and got a huge, disjointed response — effectively one sentence
or short paragraph per retrieved passage, narrated roughly in retrieval order ("Alyosha helps...
Alyosha meets... Alyosha notices... Alyosha is summoned..."), and despite the length, it still
missed the actually defining facts about who Alyosha is (buried among many minor scene fragments).

**Root cause:** "write as many connected sentences as it takes to cover what's actually there" was
read by the model as "cover every retrieved passage," not "write as much as needed to say what
matters." With only 3 passages (the old fixed retrieval) that distinction rarely showed up; with
~20 topically-scattered passages (Issue 11's HIGH-COUNT regime) it collapsed into a
passage-by-passage digest — the exact one-fact/one-scene-per-sentence failure mode Issue 8's
SYNTHESIS rule was originally written to prevent, just recurring one level up (per-passage instead
of per-fact) once retrieval volume grew.

**Fix (shipped):** SYNTHESIS now explicitly forbids working through passages one by one or in
retrieval order, explicitly names the failure mode ("if you notice yourself writing a new sentence
for each scene... stop"), and states plainly that being in the context doesn't mean a passage
belongs in the answer — the model must select the handful of things that define the person and
drop the rest. LENGTH is no longer unbounded: 1-2 sentences if sparse, otherwise a hard cap of 5-8
sentences regardless of how much material was retrieved. This keeps the length flexible between
regimes (Issue 11's original goal) without re-opening the door to exhaustive passage-by-passage
coverage.

**Not yet verified against real output** — no logcat/live-app check yet, and **not yet deployed**
(`.ts` prompt changes require `firebase deploy --only functions:quickChat` to take effect, per the
existing deployment note in Session 1).

File: `functions/src/ai/chat/quickChat.ts` (`WHO_IS_THIS` prompt).
