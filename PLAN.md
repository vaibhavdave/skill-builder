# Skill Builder — Implementation Plan

A personal learning companion: create a **skill** you want to learn, then attach
**documents** (books/PDFs/docs) and **course links** so you can refer back to
them whenever you need.

---

## 1. Tech Stack

| Layer      | Choice                                                                 |
|------------|------------------------------------------------------------------------|
| Backend    | Java 21, Spring Boot 4.0.x (Maven), Spring MVC, Spring Data JPA        |
| Database   | PostgreSQL 16, Flyway migrations                                        |
| Frontend   | React 19 + TypeScript + Vite, React Router, TanStack Query, Tailwind CSS |
| Files      | Local filesystem (configurable dir); Postgres stores metadata only      |
| Dev infra  | `docker-compose.yml` for PostgreSQL; README with run instructions       |

### Decisions taken by default (easy to change — please confirm)

1. **Single user, no login** for v1. The data model doesn't bake in a user
   assumption anywhere that would make adding auth later painful.
2. **File storage on local disk** under `app.storage.root` (default
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
```

---

## 4. REST API

Base path `/api`. JSON everywhere except uploads/downloads.

### Skills
- `GET    /skills` — list (with counts of docs/links)
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

Errors use RFC 7807 `ProblemDetail` (built into Spring).

---

## 5. Frontend (React)

### Pages
- **Dashboard `/`** — grid of skill cards (name, description, counts),
  “New Skill” dialog, delete/edit.
- **Skill detail `/skills/:id`** — header + two tabs:
  - **Documents** — drag-and-drop / file-picker upload with progress, list with
    type icon, size, notes; download & delete.
  - **Courses** — add link form (URL, title, provider, notes), list with status
    chips (To start / In progress / Completed) toggled inline.

### Stack details
- Vite dev server proxies `/api` → `localhost:8080` (no CORS pain).
- TanStack Query for all server state; no Redux needed.
- Tailwind CSS for styling; small reusable components (Card, Tabs, StatusBadge,
  FileDropzone).

---

## 6. Build Order

1. **Scaffold** — repo layout, docker-compose (Postgres), Spring Boot 4 app with
   Flyway `V1__init.sql`, health endpoint; Vite React app shell.
2. **Skills CRUD** — backend + dashboard UI.
3. **Documents** — upload/store/download/delete + Documents tab.
4. **Course links** — CRUD + Courses tab.
5. **Polish & docs** — error handling, empty states, README (setup, env vars,
   run commands), seed data script.

Each milestone is committed separately so progress is reviewable.

---

## 7. Verification

- Backend: `mvn verify` — unit tests for services, `@SpringBootTest` +
  Testcontainers(Postgres) integration tests for each controller.
- Frontend: `npm run build` + Vitest component tests for the two tabs.
- End-to-end smoke: `docker compose up postgres`, run backend + frontend, then
  create a skill → upload a PDF → add a course link → verify download and
  status updates.

---

## 8. Prerequisites you'll need at runtime

- Docker (for Postgres) or a local PostgreSQL 16
- Java 21, Node 20+
