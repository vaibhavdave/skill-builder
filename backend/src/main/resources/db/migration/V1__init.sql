CREATE TABLE skill (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document (
    id                BIGSERIAL PRIMARY KEY,
    skill_id          BIGINT NOT NULL REFERENCES skill (id) ON DELETE CASCADE,
    title             VARCHAR(300),
    original_filename VARCHAR(300) NOT NULL,
    stored_path       VARCHAR(500) NOT NULL,
    content_type      VARCHAR(150),
    size_bytes        BIGINT NOT NULL,
    notes             TEXT,
    uploaded_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_skill ON document (skill_id);

CREATE TABLE course_link (
    id         BIGSERIAL PRIMARY KEY,
    skill_id   BIGINT NOT NULL REFERENCES skill (id) ON DELETE CASCADE,
    url        VARCHAR(1000) NOT NULL,
    title      VARCHAR(300),
    provider   VARCHAR(100),
    notes      TEXT,
    status     VARCHAR(20) NOT NULL DEFAULT 'TO_START',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_course_link_skill ON course_link (skill_id);

CREATE TABLE recommendation (
    id            BIGSERIAL PRIMARY KEY,
    skill_id      BIGINT NOT NULL REFERENCES skill (id) ON DELETE CASCADE,
    resource_type VARCHAR(20) NOT NULL,
    title         VARCHAR(300) NOT NULL,
    creator       VARCHAR(200),
    reason        TEXT,
    stars         SMALLINT NOT NULL,
    search_query  VARCHAR(300),
    generated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendation_skill ON recommendation (skill_id);
