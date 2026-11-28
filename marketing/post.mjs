#!/usr/bin/env node
// Quill social poster: Bluesky, Mastodon, Instagram and Threads through their official APIs.
// No dependencies (Node 22+). See marketing/README.md for setup.
//
//   node marketing/post.mjs                 post the next due item in posts.json
//   node marketing/post.mjs --dry-run       show what would be posted, send nothing
//   node marketing/post.mjs --id <id>       post one specific item (ignores date and draft)
//   node marketing/post.mjs --only bluesky,mastodon
//   node marketing/post.mjs --text "Hello" [--image path.jpg --alt "..."]   one-off post
//   node marketing/post.mjs list            queue with per-platform status
//   node marketing/post.mjs validate        check every post offline (length, images)
//   node marketing/post.mjs check           verify each platform's credentials
//   node marketing/post.mjs refresh         refresh the Instagram and Threads tokens
//   node marketing/post.mjs threads-token [--code <code>]   get a Threads token (one-time setup)

import { readFile, writeFile, appendFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

const DIR = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(DIR, '..');
const ENV_FILE = path.join(DIR, '.env');
const POSTS_FILE = path.join(DIR, 'posts.json');
const STATE_FILE = path.join(DIR, 'state.json');
const IN_ACTIONS = process.env.GITHUB_ACTIONS === 'true';

if (existsSync(ENV_FILE)) process.loadEnvFile(ENV_FILE);

const { values: opts, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    'dry-run': { type: 'boolean', default: false },
    force: { type: 'boolean', default: false },
    only: { type: 'string' },
    id: { type: 'string' },
    text: { type: 'string' },
    image: { type: 'string' },
    alt: { type: 'string' },
    lang: { type: 'string' },
    code: { type: 'string' },
    help: { type: 'boolean', short: 'h' },
  },
});

const env = (name) => process.env[name]?.trim() || undefined;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const graphemes = (s) => [...new Intl.Segmenter().segment(s)].length;
const today = () => new Date().toISOString().slice(0, 10); // UTC

const PLATFORMS = ['bluesky', 'mastodon', 'threads', 'instagram'];
const LIMITS = {
  bluesky: { chars: 300, maxBytes: 1_000_000, types: ['image/jpeg', 'image/png', 'image/webp'] },
  mastodon: { chars: Number(env('MASTODON_MAX_CHARS') ?? 500), maxBytes: 16_000_000, types: ['image/jpeg', 'image/png', 'image/webp', 'image/gif'] },
  threads: { chars: 500, maxTags: 1, maxBytes: 8_000_000, types: ['image/jpeg', 'image/png'] },
  instagram: { chars: 2200, maxTags: 30, maxBytes: 8_000_000, types: ['image/jpeg'], needsImage: true, ratio: [0.8, 1.91] },
};

// ---------------------------------------------------------------- HTTP

const redact = (s) => String(s).replace(/(access_token|client_secret)=[^&\s]+/g, '$1=***');

async function http(url, { method = 'GET', headers = {}, json, form, body } = {}) {
  const init = { method, headers: { 'User-Agent': 'quill-social-poster', ...headers } };
  if (json !== undefined) {
    init.body = JSON.stringify(json);
    init.headers['Content-Type'] = 'application/json';
  } else if (form) {
    init.body = new URLSearchParams(Object.entries(form).filter(([, v]) => v !== undefined && v !== ''));
  } else if (body !== undefined) {
    init.body = body;
  }
  const res = await fetch(url, init);
  const text = await res.text();
  let data;
  try { data = text ? JSON.parse(text) : {}; } catch { data = { raw: text }; }
  if (!res.ok) {
    const e = data?.error;
    const msg = (typeof e === 'object' ? e?.message : e) ?? data?.message ?? text.slice(0, 300);
    throw new Error(redact(`${method} ${url.split('?')[0]} -> ${res.status}: ${msg}`));
  }
  return { status: res.status, data };
}

async function waitFor(label, fn, { tries = 30, every = 3000 } = {}) {
  for (let i = 0; i < tries; i++) {
    const done = await fn();
    if (done) return done;
    await sleep(every);
  }
  throw new Error(`${label}: timed out waiting for processing`);
}

// ---------------------------------------------------------------- Images

function sniffType(b) {
  if (b[0] === 0x89 && b[1] === 0x50 && b[2] === 0x4e && b[3] === 0x47) return 'image/png';
  if (b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff) return 'image/jpeg';
  if (b.toString('ascii', 0, 4) === 'RIFF' && b.toString('ascii', 8, 12) === 'WEBP') return 'image/webp';
  if (b.toString('ascii', 0, 3) === 'GIF') return 'image/gif';
  return 'application/octet-stream';
}

function dimensions(b, type) {
  if (type === 'image/png') return { width: b.readUInt32BE(16), height: b.readUInt32BE(20) };
  if (type === 'image/gif') return { width: b.readUInt16LE(6), height: b.readUInt16LE(8) };
  if (type === 'image/jpeg') {
    let i = 2;
    while (i + 9 < b.length) {
      if (b[i] !== 0xff) { i++; continue; }
      const m = b[i + 1];
      if (m === 0xff) { i++; continue; }
      if (m === 0xd8 || m === 0x01 || (m >= 0xd0 && m <= 0xd7)) { i += 2; continue; }
      const isSOF = m >= 0xc0 && m <= 0xcf && m !== 0xc4 && m !== 0xc8 && m !== 0xcc;
      if (isSOF) return { height: b.readUInt16BE(i + 5), width: b.readUInt16BE(i + 7) };
      i += 2 + b.readUInt16BE(i + 2);
    }
  }
  return { width: undefined, height: undefined };
}

async function loadImage(absPath) {
  const bytes = await readFile(absPath);
  const type = sniffType(bytes);
  return { file: absPath, bytes, type, ...dimensions(bytes, type) };
}

// Instagram and Threads fetch images from a public URL, so images are served from the repo on GitHub.
function publicUrl(absPath) {
  const rel = path.relative(ROOT, absPath);
  if (rel.startsWith('..') || path.isAbsolute(rel)) return undefined;
  const base = env('PUBLIC_BASE_URL') ??
    `https://raw.githubusercontent.com/${env('GITHUB_REPOSITORY') ?? 'MohammadAliUstad/Quill'}/main`;
  return `${base.replace(/\/$/, '')}/${rel.split(path.sep).map(encodeURIComponent).join('/')}`;
}

async function assertReachable(url, label) {
  if (!url) throw new Error(`${label} needs the image inside the repo so it has a public URL`);
  const res = await fetch(url, { method: 'HEAD' });
  if (!res.ok) throw new Error(`${label}: image not reachable at ${url} (${res.status}). Commit and push it first.`);
}

// ---------------------------------------------------------------- Bluesky

function blueskyFacets(text) {
  const enc = new TextEncoder();
  const at = (i) => enc.encode(text.slice(0, i)).length;
  const facets = [];
  for (const m of text.matchAll(/https?:\/\/[^\s]+/g)) {
    const uri = m[0].replace(/[.,!?:;)'"]+$/, '');
    facets.push({
      index: { byteStart: at(m.index), byteEnd: at(m.index + uri.length) },
      features: [{ $type: 'app.bsky.richtext.facet#link', uri }],
    });
  }
  for (const m of text.matchAll(/(^|\s)#([\p{L}\p{N}_]*[\p{L}_][\p{L}\p{N}_]*)/gu)) {
    const start = m.index + m[1].length;
    facets.push({
      index: { byteStart: at(start), byteEnd: at(start + 1 + m[2].length) },
      features: [{ $type: 'app.bsky.richtext.facet#tag', tag: m[2] }],
    });
  }
  return facets;
}

const bluesky = {
  name: 'Bluesky',
  configured: () => Boolean(env('BLUESKY_HANDLE') && env('BLUESKY_APP_PASSWORD')),
  async session() {
    const service = (env('BLUESKY_SERVICE') ?? 'https://bsky.social').replace(/\/$/, '');
    const { data } = await http(`${service}/xrpc/com.atproto.server.createSession`, {
      method: 'POST',
      json: { identifier: env('BLUESKY_HANDLE').replace(/^@/, ''), password: env('BLUESKY_APP_PASSWORD') },
    });
    return { service, did: data.did, handle: data.handle, auth: { Authorization: `Bearer ${data.accessJwt}` } };
  },
  async check() {
    return `@${(await this.session()).handle}`;
  },
  async post({ text, image, alt, lang }) {
    const s = await this.session();
    const record = { $type: 'app.bsky.feed.post', text, createdAt: new Date().toISOString(), langs: [lang] };
    const facets = blueskyFacets(text);
    if (facets.length) record.facets = facets;
    if (image) {
      const { data } = await http(`${s.service}/xrpc/com.atproto.repo.uploadBlob`, {
        method: 'POST', headers: { ...s.auth, 'Content-Type': image.type }, body: image.bytes,
      });
      const img = { alt: alt ?? '', image: data.blob };
      if (image.width && image.height) img.aspectRatio = { width: image.width, height: image.height };
      record.embed = { $type: 'app.bsky.embed.images', images: [img] };
    }
    const { data } = await http(`${s.service}/xrpc/com.atproto.repo.createRecord`, {
      method: 'POST', headers: s.auth, json: { repo: s.did, collection: 'app.bsky.feed.post', record },
    });
    return `https://bsky.app/profile/${s.handle}/post/${data.uri.split('/').pop()}`;
  },
};

// ---------------------------------------------------------------- Mastodon

const mastodon = {
  name: 'Mastodon',
  configured: () => Boolean(env('MASTODON_URL') && env('MASTODON_TOKEN')),
  base: () => env('MASTODON_URL').replace(/\/$/, ''),
  auth: () => ({ Authorization: `Bearer ${env('MASTODON_TOKEN')}` }),
  async check() {
    const { data } = await http(`${this.base()}/api/v1/accounts/verify_credentials`, { headers: this.auth() });
    return `@${data.acct} on ${new URL(this.base()).host}`;
  },
  async post({ text, image, alt, lang, key }) {
    const media_ids = [];
    if (image) {
      const fd = new FormData();
      fd.append('file', new Blob([image.bytes], { type: image.type }), path.basename(image.file));
      if (alt) fd.append('description', alt);
      const up = await http(`${this.base()}/api/v2/media`, { method: 'POST', headers: this.auth(), body: fd });
      const id = up.data.id;
      if (!(up.status === 200 && up.data.url)) {
        await waitFor('Mastodon media', async () => {
          const r = await http(`${this.base()}/api/v1/media/${id}`, { headers: this.auth() });
          return r.status === 200 && r.data.url;
        }, { every: 2000 });
      }
      media_ids.push(id);
    }
    const { data } = await http(`${this.base()}/api/v1/statuses`, {
      method: 'POST',
      headers: { ...this.auth(), 'Idempotency-Key': key },
      json: { status: text, media_ids, visibility: 'public', language: lang },
    });
    return data.url;
  },
};

// ---------------------------------------------------------------- Meta (Instagram, Threads)

// Instagram and Threads both work the same way: create a container, wait for it, publish it.
function metaPlatform({ name, envName, base, createPath, statusField, publishPath, refreshUrl, refreshGrant, containerFields }) {
  return {
    name,
    envName,
    configured: () => Boolean(env(envName)),
    token: () => env(envName),
    async me() {
      const fields = name === 'Instagram' ? 'user_id,username' : 'id,username';
      const { data } = await http(`${base()}/me?fields=${fields}&access_token=${this.token()}`);
      return { id: data.user_id ?? data.id, username: data.username };
    },
    async check() {
      return `@${(await this.me()).username}`;
    },
    async post({ text, image, alt }) {
      const me = await this.me();
      const imageUrl = image ? publicUrl(image.file) : undefined;
      if (image) await assertReachable(imageUrl, name);
      const { data: c } = await http(`${base()}/${me.id}/${createPath}`, {
        method: 'POST',
        form: { ...containerFields({ text, imageUrl, alt }), access_token: this.token() },
      });
      await waitFor(`${name} container`, async () => {
        const { data } = await http(`${base()}/${c.id}?fields=${statusField}&access_token=${this.token()}`);
        const status = data[statusField];
        if (status === 'ERROR' || status === 'EXPIRED') throw new Error(`${name} rejected the post (${status}${data.error_message ? `: ${data.error_message}` : ''})`);
        return status === 'FINISHED';
      });
      const { data: p } = await http(`${base()}/${me.id}/${publishPath}`, {
        method: 'POST', form: { creation_id: c.id, access_token: this.token() },
      });
      try {
        const { data } = await http(`${base()}/${p.id}?fields=permalink&access_token=${this.token()}`);
        return data.permalink ?? p.id;
      } catch {
        return p.id;
      }
    },
    async refresh() {
      const { data } = await http(`${refreshUrl}?grant_type=${refreshGrant}&access_token=${this.token()}`);
      return { token: data.access_token, days: Math.round((data.expires_in ?? 0) / 86400) };
    },
  };
}

const instagram = metaPlatform({
  name: 'Instagram',
  envName: 'INSTAGRAM_TOKEN',
  base: () => `https://graph.instagram.com/${env('INSTAGRAM_API_VERSION') ?? 'v23.0'}`,
  createPath: 'media',
  statusField: 'status_code',
  publishPath: 'media_publish',
  refreshUrl: 'https://graph.instagram.com/refresh_access_token',
  refreshGrant: 'ig_refresh_token',
  containerFields: ({ text, imageUrl, alt }) => ({ image_url: imageUrl, caption: text, alt_text: alt }),
});

const threads = metaPlatform({
  name: 'Threads',
  envName: 'THREADS_TOKEN',
  base: () => 'https://graph.threads.net/v1.0',
  createPath: 'threads',
  statusField: 'status',
  publishPath: 'threads_publish',
  refreshUrl: 'https://graph.threads.net/refresh_access_token',
  refreshGrant: 'th_refresh_token',
  containerFields: ({ text, imageUrl, alt }) => imageUrl
    ? { media_type: 'IMAGE', image_url: imageUrl, text, alt_text: alt }
    : { media_type: 'TEXT', text },
});

const API = { bluesky, mastodon, threads, instagram };

// ---------------------------------------------------------------- Queue and state

async function loadQueue() {
  const { posts } = JSON.parse(await readFile(POSTS_FILE, 'utf8'));
  return posts;
}

async function loadState() {
  if (!existsSync(STATE_FILE)) return { posted: {} };
  const s = JSON.parse(await readFile(STATE_FILE, 'utf8'));
  s.posted ??= {};
  return s;
}

const saveState = (s) => writeFile(STATE_FILE, JSON.stringify(s, null, 2) + '\n');

function onlyFilter() {
  if (!opts.only) return () => true;
  const wanted = opts.only.split(',').map((s) => s.trim().toLowerCase()).filter(Boolean);
  for (const w of wanted) if (!PLATFORMS.includes(w)) throw new Error(`Unknown platform "${w}". Use: ${PLATFORMS.join(', ')}`);
  return (p) => wanted.includes(p);
}

function compose(post, platform) {
  const o = post.overrides?.[platform] ?? {};
  const text = (o.text ?? post.text ?? '').trim();
  const tags = (o.tags ?? post.tags ?? []).slice(0, LIMITS[platform].maxTags ?? Infinity);
  const tagLine = tags.map((t) => '#' + String(t).replace(/^#/, '')).join(' ');
  return tagLine ? `${text}\n\n${tagLine}` : text;
}

const imagePathOf = (post) => post.image && (post.oneOff ? path.resolve(post.image) : path.resolve(DIR, post.image));

async function problems(post, platform) {
  const errors = [];
  const warnings = [];
  const L = LIMITS[platform];
  const text = compose(post, platform);
  const n = graphemes(text);
  if (!text) errors.push('empty text');
  if (n > L.chars) errors.push(`${n}/${L.chars} characters`);
  const imgPath = imagePathOf(post);
  if (!imgPath) {
    if (L.needsImage) errors.push('needs an image');
    return { errors, warnings, text };
  }
  if (!existsSync(imgPath)) {
    errors.push(`image not found: ${path.relative(process.cwd(), imgPath)}`);
    return { errors, warnings, text };
  }
  const img = await loadImage(imgPath);
  if (!L.types.includes(img.type)) errors.push(`image must be ${L.types.map((t) => t.split('/')[1]).join(' or ')}, got ${img.type.split('/')[1]}`);
  if (img.bytes.length > L.maxBytes) errors.push(`image is ${(img.bytes.length / 1e6).toFixed(2)} MB, limit ${L.maxBytes / 1e6} MB`);
  if (L.ratio && img.width && img.height) {
    const r = img.width / img.height;
    if (r < L.ratio[0] || r > L.ratio[1]) errors.push(`image ratio ${img.width}x${img.height} must be between 4:5 and 1.91:1 (1080x1350 works)`);
  }
  if (!post.alt) warnings.push('no alt text');
  return { errors, warnings, text, img };
}

// ---------------------------------------------------------------- Commands

async function publish(post, targets, state) {
  const results = [];
  const lang = post.lang ?? 'en';
  // Check everything before sending anything, so a bad post never goes out to half the platforms.
  const checked = {};
  for (const p of targets) {
    checked[p] = await problems(post, p);
    if (checked[p].errors.length) throw new Error(`${post.id ?? 'post'} on ${API[p].name}: ${checked[p].errors.join('; ')}`);
  }

  for (const p of targets) {
    const { text, img, warnings } = checked[p];
    if (opts['dry-run']) {
      console.log(`\n--- ${API[p].name} (${graphemes(text)}/${LIMITS[p].chars})${warnings.length ? `  [${warnings.join(', ')}]` : ''}`);
      console.log(text);
      if (img) console.log(`[image] ${path.relative(ROOT, img.file)} ${img.width}x${img.height}${p === 'instagram' || p === 'threads' ? ` -> ${publicUrl(img.file) ?? 'NO PUBLIC URL'}` : ''}`);
      continue;
    }
    try {
      const key = `quill-${post.id ?? 'oneoff'}-${p}${opts.force || post.oneOff ? `-${Date.now()}` : ''}`;
      const url = await API[p].post({ text, image: img, alt: post.alt, lang, key });
      console.log(`✓ ${API[p].name}: ${url}`);
      results.push({ p, ok: true });
      if (state && post.id) {
        (state.posted[post.id] ??= {})[p] = { at: new Date().toISOString(), url };
        await saveState(state);
      }
    } catch (e) {
      console.error(`✗ ${API[p].name}: ${e.message}`);
      results.push({ p, ok: false });
    }
  }
  return results;
}

async function cmdNext() {
  const only = onlyFilter();

  if (opts.text !== undefined) {
    const post = { oneOff: true, text: opts.text, image: opts.image, alt: opts.alt, lang: opts.lang };
    let targets = PLATFORMS.filter(only).filter((p) => API[p].configured());
    if (!opts.image && targets.some((p) => LIMITS[p].needsImage)) {
      console.log('Skipping Instagram: it needs an image (--image).');
      targets = targets.filter((p) => !LIMITS[p].needsImage);
    }
    if (!targets.length) throw new Error('No configured platforms selected');
    const results = await publish(post, targets, null);
    if (results.some((r) => !r.ok)) process.exitCode = 1;
    return;
  }

  const posts = await loadQueue();
  const state = await loadState();
  const done = (post, p) => !opts.force && state.posted[post.id]?.[p];
  const wanted = (post) => PLATFORMS.filter((p) => !post.platforms || post.platforms.includes(p)).filter(only);

  let post;
  if (opts.id) {
    post = posts.find((x) => x.id === opts.id);
    if (!post) throw new Error(`No post with id "${opts.id}"`);
  } else {
    post = posts.find((x) => !x.draft && (!x.date || x.date <= today()) && wanted(x).some((p) => !done(x, p)));
    if (!post) { console.log('Nothing due today.'); return; }
  }

  const pending = wanted(post).filter((p) => !done(post, p));
  const targets = pending.filter((p) => API[p].configured());
  const unconfigured = pending.filter((p) => !API[p].configured());

  console.log(`Post "${post.id}"${opts['dry-run'] ? ' (dry run)' : ''} -> ${targets.map((p) => API[p].name).join(', ') || 'nothing'}`);
  if (unconfigured.length) console.log(`Skipping (no credentials): ${unconfigured.map((p) => API[p].name).join(', ')}`);
  if (!targets.length) {
    if (!pending.length) console.log('Already posted everywhere. Use --force to post again.');
    return;
  }

  const results = await publish(post, targets, state);
  if (opts['dry-run']) return;

  // Platforms without credentials are marked skipped, so adding a platform later doesn't replay the backlog.
  // Only on scheduled-style runs (no --only), so a "--only bluesky" run leaves the others for later.
  if (!opts.only && unconfigured.length) {
    for (const p of unconfigured) (state.posted[post.id] ??= {})[p] = { at: new Date().toISOString(), skipped: 'no credentials' };
    await saveState(state);
  }

  if (results.some((r) => !r.ok)) process.exitCode = 1;
  if (!IN_ACTIONS && results.some((r) => r.ok)) {
    console.log('\nPosted from this machine: commit and push marketing/state.json, or the daily run will post it again.');
  }
}

async function cmdList() {
  const posts = await loadQueue();
  const state = await loadState();
  for (const post of posts) {
    const marks = PLATFORMS.map((p) => {
      if (post.platforms && !post.platforms.includes(p)) return `${p}:-`;
      const s = state.posted[post.id]?.[p];
      return `${p}:${s ? (s.skipped ? 'skip' : 'done') : '…'}`;
    }).join(' ');
    console.log(`${(post.date ?? 'any day').padEnd(10)}  ${post.id.padEnd(28)} ${post.draft ? '[draft] ' : ''}${marks}`);
  }
}

async function cmdValidate() {
  const posts = await loadQueue();
  let bad = 0;
  const ids = new Set();
  for (const post of posts) {
    const lines = [];
    if (!post.id) lines.push('missing id');
    else if (ids.has(post.id)) lines.push('duplicate id');
    ids.add(post.id);
    if (post.date && !/^\d{4}-\d{2}-\d{2}$/.test(post.date)) lines.push(`date must be YYYY-MM-DD, got "${post.date}"`);
    for (const p of post.platforms ?? PLATFORMS) {
      if (!PLATFORMS.includes(p)) { lines.push(`unknown platform "${p}"`); continue; }
      const { errors, warnings } = await problems(post, p);
      for (const e of errors) lines.push(`✗ ${API[p].name}: ${e}`);
      for (const w of warnings) lines.push(`! ${API[p].name}: ${w}`);
    }
    if (lines.some((l) => !l.startsWith('!'))) bad++;
    console.log(`${lines.length ? '' : '✓ '}${post.id}${lines.length ? '\n  ' + lines.join('\n  ') : ''}`);
  }
  console.log(`\n${posts.length} posts, ${bad} with errors.`);
  if (bad) process.exitCode = 1;
}

async function cmdCheck() {
  for (const p of PLATFORMS) {
    if (!API[p].configured()) { console.log(`- ${API[p].name}: not configured`); continue; }
    try { console.log(`✓ ${API[p].name}: ${await API[p].check()}`); }
    catch (e) { console.error(`✗ ${API[p].name}: ${e.message}`); process.exitCode = 1; }
  }
}

async function setEnvVar(name, value) {
  const lines = existsSync(ENV_FILE) ? (await readFile(ENV_FILE, 'utf8')).split(/\r?\n/) : [];
  const i = lines.findIndex((l) => l.startsWith(`${name}=`));
  if (i >= 0) lines[i] = `${name}=${value}`; else lines.push(`${name}=${value}`);
  await writeFile(ENV_FILE, lines.join('\n').replace(/\n*$/, '\n'));
}

async function cmdRefresh() {
  const out = env('REFRESHED_ENV');
  for (const api of [instagram, threads]) {
    if (!api.configured()) { console.log(`- ${api.name}: not configured`); continue; }
    try {
      const { token, days } = await api.refresh();
      if (IN_ACTIONS) console.log(`::add-mask::${token}`);
      if (out) await appendFile(out, `${api.envName}=${token}\n`);
      else await setEnvVar(api.envName, token);
      console.log(`✓ ${api.name}: token refreshed, valid ~${days} days`);
    } catch (e) {
      console.error(`✗ ${api.name}: ${e.message}`);
      process.exitCode = 1;
    }
  }
}

// One-time helper: turns a Threads login into a 60-day token and saves it to marketing/.env.
async function cmdThreadsToken() {
  const appId = env('THREADS_APP_ID');
  const secret = env('THREADS_APP_SECRET');
  const redirect = env('THREADS_REDIRECT_URI');
  if (!appId || !secret || !redirect) throw new Error('Set THREADS_APP_ID, THREADS_APP_SECRET and THREADS_REDIRECT_URI in marketing/.env first');
  if (!opts.code) {
    const u = new URL('https://threads.net/oauth/authorize');
    u.search = new URLSearchParams({ client_id: appId, redirect_uri: redirect, scope: 'threads_basic,threads_content_publish', response_type: 'code' });
    console.log(`Open this, approve, then copy the "code" from the address bar you land on:\n\n${u}\n\nThen run: node marketing/post.mjs threads-token --code <code>`);
    return;
  }
  const code = opts.code.replace(/#_$/, '');
  const { data: short } = await http('https://graph.threads.net/oauth/access_token', {
    method: 'POST', form: { client_id: appId, client_secret: secret, grant_type: 'authorization_code', redirect_uri: redirect, code },
  });
  const { data: long } = await http(`https://graph.threads.net/access_token?grant_type=th_exchange_token&client_secret=${secret}&access_token=${short.access_token}`);
  await setEnvVar('THREADS_TOKEN', long.access_token);
  console.log(`✓ THREADS_TOKEN saved to marketing/.env (valid ~${Math.round(long.expires_in / 86400)} days). Add the same value as a GitHub secret.`);
}

const COMMANDS = { next: cmdNext, list: cmdList, validate: cmdValidate, check: cmdCheck, refresh: cmdRefresh, 'threads-token': cmdThreadsToken };

if (opts.help) {
  console.log((await readFile(fileURLToPath(import.meta.url), 'utf8')).split('\n').slice(1, 15).map((l) => l.replace(/^\/\/ ?/, '')).join('\n'));
} else {
  const cmd = COMMANDS[positionals[0] ?? 'next'];
  if (!cmd) {
    console.error(`Unknown command "${positionals[0]}". Use: ${Object.keys(COMMANDS).join(', ')}`);
    process.exitCode = 1;
  } else {
    try { await cmd(); } catch (e) { console.error(`Error: ${e.message}`); process.exitCode = 1; }
  }
}
