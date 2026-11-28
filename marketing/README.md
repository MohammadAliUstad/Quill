# Quill social posts

Posts about Quill on **Bluesky, Mastodon, Threads and Instagram** from a queue in this folder, using each platform's official API. A GitHub Action posts the next due item once a day. You can also post from your own terminal.

| File | What it is |
| --- | --- |
| `posts.json` | The queue. Edit this to plan posts. |
| `state.json` | What has been posted where. Written by the script; commit it. |
| `images/` | Post images. They must be pushed to `main` before their post goes out, because Instagram and Threads fetch them from GitHub. |
| `post.mjs` | The poster. Node 22+, no dependencies. |
| `.env` | Your credentials for local runs (git-ignored). Copy `.env.example`. |

The workflows live in `.github/workflows/social-post.yml` (daily, 14:00 UTC) and `social-refresh-tokens.yml` (weekly).

## Writing posts

```json
{
  "id": "insights-heatmap",
  "date": "2026-12-03",
  "text": "Your reading year, one square per day...",
  "tags": ["books", "reading"],
  "image": "images/insights.jpg",
  "alt": "Quill's Insights screen showing a heatmap of reading days",
  "platforms": ["bluesky", "mastodon", "threads", "instagram"],
  "overrides": { "mastodon": { "tags": ["books", "bookstodon"] } },
  "lang": "en",
  "draft": false
}
```

- `id` must be unique. `text` is required. Everything else is optional.
- `date` (UTC, `YYYY-MM-DD`): the post goes out on the first daily run on or after that date. With no date it goes out as soon as it's next in line. One post per day, in file order.
- `tags` become hashtags on a new line at the end. Threads only uses the first one (it allows one topic per post). Instagram allows up to 30.
- `platforms` defaults to all four. `overrides.<platform>` can replace `text` and/or `tags` for one platform.
- `draft: true` keeps a post out of the daily run.

**Limits the script checks:**

| | Text | Image |
| --- | --- | --- |
| Bluesky | 300 characters | Under 1 MB |
| Mastodon | 500 characters | |
| Threads | 500 characters | JPEG or PNG |
| Instagram | 2,200 characters | **Required. JPEG only, between 4:5 and 1.91:1.** Use 1080×1350 or 1080×1080. Phone screenshots are too tall, so put them inside a designed image. |

Run `node marketing/post.mjs validate` after editing. It checks every post offline.

## Posting from the terminal

```sh
node marketing/post.mjs --dry-run            # preview the next post on every platform
node marketing/post.mjs                      # post it
node marketing/post.mjs --id insights-heatmap --only bluesky
node marketing/post.mjs --text "Quill 5.1 is out" --only bluesky,mastodon
node marketing/post.mjs list                 # queue and what's been posted where
node marketing/post.mjs check                # test every platform's login
```

After posting a queued item locally, **commit and push `state.json`**. Otherwise the daily run doesn't know about it and posts it again.

You can also start the GitHub Action from the terminal: `gh workflow run social-post.yml -f id=insights-heatmap -f dry_run=true`.

If a platform fails, the others still post. The next run retries only the failed platform. Platforms with no credentials are skipped and marked as skipped, so adding Instagram later doesn't replay old posts there.

## One-time setup

Each platform is optional. Set up one, run `check`, then move on to the next.

### 1. Bluesky
1. Log in as the Quill account. Go to **Settings → Privacy and security → App passwords → Add App Password**. Name it `quill-poster`.
2. `BLUESKY_HANDLE` = the handle, e.g. `quillreader.bsky.social`. `BLUESKY_APP_PASSWORD` = the generated password.

### 2. Mastodon
1. Log in as the Quill account. Go to **Preferences → Development → New application**. Name it `quill-poster`.
2. Scopes: untick everything, then tick `read:accounts`, `write:statuses` and `write:media`. Submit.
3. Open the application and copy **Your access token**.
4. `MASTODON_URL` = your server, e.g. `https://mastodon.social`. `MASTODON_TOKEN` = the token.

### 3. Instagram
1. In the Instagram app, switch the Quill account to a **Professional account** (Creator or Business). The API can't post from personal accounts.
2. Go to [developers.facebook.com](https://developers.facebook.com/apps) → **Create app**. Pick the use case **"Manage messaging & content on Instagram"**. Business portfolio: none is fine.
3. In the app, open **Instagram → API setup with Instagram login**. Under **Generate access tokens**, click **Add account** and log in as Quill. Grant the content-publishing permission, then click **Generate token**.
4. `INSTAGRAM_TOKEN` = that token. It lasts 60 days, and the weekly workflow keeps renewing it.

The app can stay in **Development mode**. That's enough to post to accounts that have a role on the app, so you don't need App Review.

### 4. Threads
1. In the same Meta developer dashboard, add the use case **"Access the Threads API"**. Enable `threads_basic` and `threads_content_publish`.
2. Under **App roles → Roles**, add the Quill Threads account as a **Threads Tester**. Then accept the invite in Threads: **Settings → Account → Website permissions → Invites**.
3. Get a token:
   - If the Threads use case settings show a **User Token Generator**, generate a token for the Quill account there.
   - Otherwise, use the helper. In the Threads use case settings, add `https://github.com/MohammadAliUstad/Quill` as a redirect callback URL. Put `THREADS_APP_ID`, `THREADS_APP_SECRET` (the Threads app ID and secret) and `THREADS_REDIRECT_URI` in `marketing/.env`. Then run `node marketing/post.mjs threads-token`, open the link, approve, and copy the `code=` value from the address bar you land on. Finally, run `node marketing/post.mjs threads-token --code <that code>`. It saves a 60-day `THREADS_TOKEN` to `.env`.

### 5. GitHub
1. **Repo → Settings → Secrets and variables → Actions → New repository secret.** Add whichever of these you have: `BLUESKY_HANDLE`, `BLUESKY_APP_PASSWORD`, `MASTODON_URL`, `MASTODON_TOKEN`, `INSTAGRAM_TOKEN`, `THREADS_TOKEN`.
2. For the token refresh: create a [fine-grained personal access token](https://github.com/settings/personal-access-tokens/new) for this repository only, with **Secrets: Read and write**. Save it as the secret `GH_SECRETS_TOKEN`.
3. **Actions → Social post → Run workflow** with *dry run* ticked, to confirm it works.

From the terminal, `gh secret set BLUESKY_APP_PASSWORD` (it prompts for the value) does the same as step 1.

## Notes

- Instagram and Threads post the image from `https://raw.githubusercontent.com/MohammadAliUstad/Quill/main/marketing/images/...`. That URL only works once the image is pushed.
- The `INSTAGRAM_API_VERSION` env var overrides the Graph API version (default `v23.0`) if Meta retires it.
- If a refresh fails (for example, the token was revoked), the weekly workflow fails and GitHub emails you. Generate a new token and update the secret.
