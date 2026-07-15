# 🎯 Skill Builder

A personal learning companion. Create a **skill** you want to learn, then keep
everything for it in one place:

- **📄 Documents** — upload books, PDFs and docs; view PDFs inline in the
  browser, download anything back whenever you need it.
- **🔗 Courses** — save links to the courses you plan to study and track each
  one (To start → In progress → Completed).
- **⭐ Recommended** — Claude curates the best books, courses and YouTube
  videos for the skill, each with a 1–5 star rating of its learning value and
  a reliable "Find it" search link.
- **🌗 Light / dark / system theme**, persisted across visits.

| Layer    | Tech |
|----------|------|
| Backend  | Java 21, Spring Boot 4, Spring Data JPA, Flyway |
| Database | PostgreSQL 16 |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4, TanStack Query |
| AI       | Anthropic Java SDK — `claude-sonnet-5`, effort `high`, structured JSON output |

## Running locally

Prerequisites: Java 21, Node 20+, Docker (or a local PostgreSQL 16).

```bash
# 1. Database
docker compose up -d postgres

# 2. Backend (http://localhost:8080)
export ANTHROPIC_API_KEY=sk-ant-...   # optional — only needed for the Recommended tab
cd backend && mvn spring-boot:run

# 3. Frontend (http://localhost:5173, proxies /api to the backend)
cd frontend && npm install && npm run dev
```

Open <http://localhost:5173>.

Without `ANTHROPIC_API_KEY` everything works except recommendation generation,
which returns a clear message telling you to configure the key.

### Configuration

| Environment variable | Default | Purpose |
|----------------------|---------|---------|
| `DB_URL`             | `jdbc:postgresql://localhost:5448/skillbuilder` | Postgres JDBC URL |
| `DB_USERNAME`        | `skillbuilder` | DB user |
| `DB_PASSWORD`        | `skillbuilder` | DB password |
| `STORAGE_ROOT`       | `./data/uploads` | Where uploaded files are stored on disk |
| `ANTHROPIC_API_KEY`  | — | Enables the Claude-powered Recommended tab |

Uploaded files live on disk under `STORAGE_ROOT/{skillId}/`; Postgres stores
only metadata. The schema is created automatically by Flyway on first start.

## API overview

Base path `/api`. Errors use RFC 7807 `application/problem+json`.

| Method & path | Purpose |
|---|---|
| `GET/POST /skills`, `GET/PUT/DELETE /skills/{id}` | Skills CRUD (list includes per-skill counts) |
| `GET/POST /skills/{id}/documents` | List / upload (multipart `file`, optional `title`, `notes`) |
| `GET /documents/{id}/view` | Stream inline (PDFs open in the browser viewer) |
| `GET /documents/{id}/download` | Download with original filename |
| `PUT/DELETE /documents/{id}` | Edit metadata / delete (removes the disk file) |
| `GET/POST /skills/{id}/links`, `PUT/DELETE /links/{id}` | Course links with `TO_START / IN_PROGRESS / COMPLETED` status |
| `GET /skills/{id}/recommendations` | Stored Claude recommendations |
| `POST /skills/{id}/recommendations/generate` | Ask Claude for a fresh set (replaces the stored one) |

## Development

```bash
cd backend && mvn test         # backend unit tests
cd frontend && npm run build   # type-check + production build
```

Design notes:

- Recommendations use the Anthropic SDK's structured JSON output with an
  explicit schema, so responses always parse; stars are clamped to 1–5.
- Claude is asked for a `searchQuery` per resource instead of raw URLs —
  model-remembered links are often stale, search links never are.
- The Claude call runs outside any DB transaction; the stored set is swapped
  in a short transaction afterwards.
- Deleting a skill cascades in the DB and removes its upload directory.
