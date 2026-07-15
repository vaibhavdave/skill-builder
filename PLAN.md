# Skill Builder — Implementation Plan

A personal learning companion: create a **skill** you want to learn, then attach
**documents** (books/PDFs/docs), **course links**, and **video transcriptions**
(auto-transcribed to English with an AI summary, delivered as Markdown).

---

## 1. Tech Stack

| Layer      | Choice                                                                 |
|------------|------------------------------------------------------------------------|
| Backend    | Java 21, Spring Boot 4.0.x (Maven), Spring MVC, Spring Data JPA        |
| Database   | PostgreSQL 16, Flyway migrations                                        |
| Frontend   | React 19 + TypeScript + Vite, React Router, TanStack Query, Tailwind CSS |
| Transcribe | `yt-dlp` (audio/caption extraction) + OpenAI Whisper API (speech→English) |
| Summary    | Anthropic Claude API (`claude-sonnet-5`)                                |
| Files      | Local filesystem (configurable dir); Postgres stores metadata only      |
| Dev infra  | `docker-compose.yml` for PostgreSQL; README with run instructions       |

### Decisions taken by default (easy to change — please confirm)

1. **Transcription engine — captions first, Whisper fallback**: for YouTube
   links, fetch existing subtitles via `yt-dlp` (free/fast); if none exist,
   download audio and send to OpenAI's Whisper transcription/translation API
   (needs `OPENAI_API_KEY`, ~$0.006/min). Non-YouTube direct video URLs go
   straight to Whisper. Translation to English is handled by Whisper's
   `translations` endpoint when source language isn't English.
2. **Summary — Claude API** (`ANTHROPIC_API_KEY`) appended as a final
   `## Summary` section of the Markdown file.
3. **Single user, no login** for v1. The data model doesn't bake in a user
   assumption anywhere that would make adding auth later painful.
4. **File storage on local disk** under `app.storage.root` (default
   `./data/uploads`), organized as `{skillId}/{uuid}-{filename}`.

---

## 2. Repository Layout

```
skill-builder/
├── backend/                  # Spring Boot 4 app (Maven)
│   ├── pom.xml
│   └── src/main/java/com/skillbuilder/...
│   └── src/main/resources/db/migration/   # Flyway SQL
├── frontend/                 # Vite + React + TS
│   ├── package.json
│   └── src/...
├── docker-compose.yml        # postgres (+ optional app services later)
├── PLAN.md
└── README.md
```

---

## 3. Data Model (PostgreSQL)

```
skill
  id            BIGSERIAL PK
  name          VARCHAR(200) NOT NULL UNIQUE
  description   TEXT
  created_at    TIMESTAMPTZ
  updated_at    TIMESTAMPTZ

document                      -- uploaded books / PDFs / docs
  id                BIGSERIAL PK
  skill_id          FK -> skill (ON DELETE CASCADE)
  title             VARCHAR(300)
  original_filename VARCHAR(300)
  stored_path       VARCHAR(500)      -- relative to storage root
  content_type      VARCHAR(150)
  size_bytes        BIGINT
  notes             TEXT
  uploaded_at       TIMESTAMPTZ

course_link
  id            BIGSERIAL PK
  skill_id      FK -> skill (ON DELETE CASCADE)
  url           VARCHAR(1000) NOT NULL
  title         VARCHAR(300)
  provider      VARCHAR(100)          -- e.g. Udemy, Coursera, YouTube
  notes         TEXT
  status        VARCHAR(20)           -- TO_START | IN_PROGRESS | COMPLETED
  created_at    TIMESTAMPTZ

transcription
  id                BIGSERIAL PK
  skill_id          FK -> skill (ON DELETE CASCADE)
  video_url         VARCHAR(1000) NOT NULL
  video_title       VARCHAR(500)
  status            VARCHAR(20)       -- PENDING | DOWNLOADING | TRANSCRIBING
                                      -- | SUMMARIZING | COMPLETED | FAILED
  source_language   VARCHAR(50)
  markdown_path     VARCHAR(500)      -- generated .md file on disk
  error_message     TEXT
  created_at        TIMESTAMPTZ
  completed_at      TIMESTAMPTZ
```

---

## 4. REST API

Base path `/api`. JSON everywhere except uploads/downloads.

### Skills
- `GET    /skills` — list (with counts of docs/links/transcriptions)
- `POST   /skills` — create `{name, description}`
- `GET    /skills/{id}`
- `PUT    /skills/{id}`
- `DELETE /skills/{id}`

### Documents
- `POST   /skills/{id}/documents` — multipart upload (file + optional title/notes)
- `GET    /skills/{id}/documents`
- `GET    /documents/{id}/download` — streams the file with original name
- `PUT    /documents/{id}` — edit title/notes
- `DELETE /documents/{id}` — removes DB row + file

### Course links
- `POST   /skills/{id}/links` — `{url, title, provider, notes}`
- `GET    /skills/{id}/links`
- `PUT    /links/{id}` — edit fields incl. status
- `DELETE /links/{id}`

### Transcriptions
- `POST   /skills/{id}/transcriptions` — `{videoUrl}` → creates job (202 Accepted)
- `GET    /skills/{id}/transcriptions`
- `GET    /transcriptions/{id}` — status + metadata (polled by UI)
- `GET    /transcriptions/{id}/markdown` — raw markdown (for in-app rendering)
- `GET    /transcriptions/{id}/download` — `.md` file download
- `DELETE /transcriptions/{id}`

Errors use RFC 7807 `ProblemDetail` (built into Spring).

---

## 5. Transcription Pipeline (async)

Runs on a Spring `@Async` executor; UI polls job status.

1. **PENDING** → job row created, worker picks it up.
2. **DOWNLOADING** — `yt-dlp` fetches video metadata (title, language) and:
   - if human/auto captions exist → download subtitles (`.vtt`) directly;
   - else → extract audio (`m4a`, chunked if >25 MB API limit).
3. **TRANSCRIBING** — captions are cleaned into text; or audio is sent to
   OpenAI Whisper (`/audio/translations` for non-English → English).
4. **SUMMARIZING** — transcript sent to Claude for a structured summary
   (key points, takeaways).
5. **COMPLETED** — Markdown file written:

```markdown
# {Video Title}

> Source: {url}
> Transcribed: {date} · Detected language: {lang} · Duration: {mm:ss}

## Transcript
{paragraphized English transcript}

## Summary
{Claude-generated summary}
```

Failures at any step → **FAILED** with a human-readable `error_message`
(retry available from the UI). API keys are read from environment variables
(`OPENAI_API_KEY`, `ANTHROPIC_API_KEY`) — never committed.

---

## 6. Frontend (React)

### Pages
- **Dashboard `/`** — grid of skill cards (name, description, counts),
  “New Skill” dialog, delete/edit.
- **Skill detail `/skills/:id`** — header + three tabs:
  - **Documents** — drag-and-drop / file-picker upload with progress, list with
    type icon, size, notes; download & delete.
  - **Courses** — add link form (URL, title, provider, notes), list with status
    chips (To start / In progress / Completed) toggled inline.
  - **Transcriber** — paste video URL → job appears with live status badge
    (polling via TanStack Query `refetchInterval` while active); completed jobs
    open a rendered Markdown viewer (`react-markdown`) with a download button.

### Stack details
- Vite dev server proxies `/api` → `localhost:8080` (no CORS pain).
- TanStack Query for all server state; no Redux needed.
- Tailwind CSS for styling; small reusable components (Card, Tabs, StatusBadge,
  FileDropzone, MarkdownViewer).

---

## 7. Build Order

1. **Scaffold** — repo layout, docker-compose (Postgres), Spring Boot 4 app with
   Flyway `V1__init.sql`, health endpoint; Vite React app shell.
2. **Skills CRUD** — backend + dashboard UI.
3. **Documents** — upload/store/download/delete + Documents tab.
4. **Course links** — CRUD + Courses tab.
5. **Transcriber** — yt-dlp integration, Whisper + Claude clients, async job
   runner, markdown generation + Transcriber tab with polling & viewer.
6. **Polish & docs** — error handling, empty states, README (setup, env vars,
   run commands), seed data script.

Each milestone is committed separately so progress is reviewable.

---

## 8. Verification

- Backend: `mvn verify` — unit tests for services, `@SpringBootTest` +
  Testcontainers(Postgres) integration tests for each controller; transcription
  pipeline tested with stubbed yt-dlp/Whisper/Claude clients.
- Frontend: `npm run build` + Vitest component tests for the three tabs.
- End-to-end smoke: `docker compose up postgres`, run backend + frontend, then
  create a skill → upload a PDF → add a course link → transcribe a short
  captioned YouTube video → download the generated Markdown.

---

## 9. Prerequisites you'll need at runtime

- Docker (for Postgres) or a local PostgreSQL 16
- Java 21, Node 20+
- `yt-dlp` + `ffmpeg` on PATH (backend shells out to them)
- `OPENAI_API_KEY` (Whisper fallback) and `ANTHROPIC_API_KEY` (summaries) —
  transcriber degrades gracefully: captions-only mode works with no keys, but
  then videos without captions fail and summaries are skipped.
