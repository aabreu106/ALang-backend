-- ===========================================================================
-- V1: Initial schema for ALang language-learning backend
--
-- Column names follow Spring Boot's SpringPhysicalNamingStrategy:
--   Java camelCase → SQL snake_case
--
-- String @Id fields map to VARCHAR(255) by default in Hibernate.
-- UUID primary keys use GenerationType.UUID (app-generated, stored as VARCHAR).
-- ===========================================================================

-- ===========================
-- Independent tables (no FKs)
-- ===========================

CREATE TABLE languages (
    code            VARCHAR(255) PRIMARY KEY,  -- ISO 639-1: "ja", "es", "fr"
    name            VARCHAR(255) NOT NULL,
    native_name     VARCHAR(255) NOT NULL,
    fully_supported BOOLEAN      DEFAULT TRUE,
    preferred_model VARCHAR(255)
);

CREATE TYPE user_tier AS ENUM ('free', 'pro');
CREATE TYPE note_type AS ENUM ('vocab', 'grammar', 'exception', 'other');
CREATE TYPE role_type AS ENUM ('user', 'assistant');
CREATE TYPE session_status AS ENUM ('active', 'closed');

CREATE TABLE users (
    id                      VARCHAR(255) PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    display_name            VARCHAR(255) NOT NULL,
    app_language_code       VARCHAR(255) NOT NULL, -- language used for UI/LLM explanations
    target_language_codes   VARCHAR(255)[] DEFAULT '{}', -- languages the user is learning
    total_daily_tokens_used BIGINT       DEFAULT 0,
    last_token_reset_date   TIMESTAMP,
    tier                    user_tier    DEFAULT 'free',
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL
);

-- ===========================
-- Notes (references users + languages)
-- ===========================

CREATE TABLE notes (
    id                    VARCHAR(255) PRIMARY KEY,
    user_id               VARCHAR(255) NOT NULL REFERENCES users(id),
    teaching_language_code VARCHAR(255) NOT NULL REFERENCES languages(code), -- language the note explains in
    learning_language_code VARCHAR(255) NOT NULL REFERENCES languages(code), -- language being taught/explained
    type                  note_type    NOT NULL,
    title                 VARCHAR(255) NOT NULL,
    summary               TEXT,
    note_content          TEXT,
    structured_content    JSONB,        -- type-specific structured data (schema varies by note_type)
    user_edited           BOOLEAN          NOT NULL DEFAULT FALSE,
    review_count          INTEGER          DEFAULT 0,
    last_reviewed_at      TIMESTAMP,
    next_review_at        TIMESTAMP,
    ease_factor           DOUBLE PRECISION DEFAULT 2.0,
    interval_days         INTEGER          DEFAULT 1,
    created_at            TIMESTAMP        NOT NULL,
    updated_at            TIMESTAMP        NOT NULL
);

-- ===========================
-- Note tags (controlled tag system for categorizing notes)
-- ===========================

CREATE TABLE note_tags (
    id           VARCHAR(255) PRIMARY KEY,
    note_id      VARCHAR(255) NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    tag_category VARCHAR(50)  NOT NULL,  -- 'topic', 'formality', 'difficulty', 'function'
    tag_value    VARCHAR(100) NOT NULL,
    UNIQUE(note_id, tag_category, tag_value)
);

-- ===========================
-- Note relations (links between related notes)
-- ===========================

CREATE TABLE note_relations (
    id             VARCHAR(255) PRIMARY KEY,
    source_note_id VARCHAR(255) NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    target_note_id VARCHAR(255) NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    relation_type  VARCHAR(50)  NOT NULL,  -- 'similar_to', 'opposite_of', 'often_confused_with', 'related'
    UNIQUE(source_note_id, target_note_id, relation_type)
);


-- ===========================
-- Conversation summaries (references users + languages. good for mainting LLM context across sessions)
-- ===========================

CREATE TABLE conversation_summaries (
    id                      VARCHAR(255) PRIMARY KEY,
    user_id                 VARCHAR(255) NOT NULL REFERENCES users(id),
    teaching_language_code  VARCHAR(255) NOT NULL REFERENCES languages(code),
    learning_language_code  VARCHAR(255) NOT NULL REFERENCES languages(code),
    summary_text            TEXT         NOT NULL,
    message_count           INTEGER      NOT NULL,
    summary_token_count     INTEGER,
    created_at              TIMESTAMP    NOT NULL,
    conversation_start_time TIMESTAMP,
    conversation_end_time   TIMESTAMP
);


-- ===========================
-- Chat sessions (groups recent messages into a single-topic conversation)
-- ===========================

CREATE TABLE chat_sessions (
    id                     VARCHAR(255) PRIMARY KEY,
    user_id                VARCHAR(255)   NOT NULL REFERENCES users(id),
    teaching_language_code VARCHAR(255)   NOT NULL REFERENCES languages(code),
    learning_language_code VARCHAR(255)   NOT NULL REFERENCES languages(code),
    status                 session_status NOT NULL DEFAULT 'active',
    title                  VARCHAR(255),         -- optional user-supplied label
    created_at             TIMESTAMP      NOT NULL,
    updated_at             TIMESTAMP      NOT NULL,
    closed_at              TIMESTAMP,             -- set when status transitions to 'closed'
    note_created           BOOLEAN        NOT NULL DEFAULT FALSE
);

-- ===========================
-- Recent messages (temporary, with TTL. Required as an active conversation buffer to send to the LLM to allow it to follow the conversation coherently)
-- ===========================

CREATE TABLE recent_messages (
    id                     VARCHAR(255) PRIMARY KEY,
    user_id                VARCHAR(255) NOT NULL REFERENCES users(id),
    teaching_language_code VARCHAR(255) NOT NULL REFERENCES languages(code),
    learning_language_code VARCHAR(255) NOT NULL REFERENCES languages(code),
    session_id             VARCHAR(255) NOT NULL REFERENCES chat_sessions(id),
    role                   role_type    NOT NULL,
    content                TEXT         NOT NULL,
    model_used             VARCHAR(255),
    token_count            INTEGER,
    created_at             TIMESTAMP    NOT NULL,
    expires_at             TIMESTAMP
);

-- ===========================
-- Review events (references users + notes. Spaced repetition analytics and algorithm tuning)
-- ===========================

CREATE TABLE review_events (
    id                     VARCHAR(255) PRIMARY KEY,
    user_id                VARCHAR(255) NOT NULL REFERENCES users(id),
    note_id                VARCHAR(255) NOT NULL REFERENCES notes(id),
    quality                INTEGER      NOT NULL,  -- 1-4 recall rating, similar to Anki's system of easy, medium, hard, etc.
    time_spent_seconds     INTEGER,
    previous_interval_days INTEGER,
    next_interval_days     INTEGER,
    ease_factor            DOUBLE PRECISION,
    reviewed_at            TIMESTAMP    NOT NULL
);

-- ===========================
-- Indexes
-- ===========================

-- Notes: query by user + learning language (most common filter)
CREATE INDEX idx_notes_user_learning_language ON notes(user_id, learning_language_code);

-- Notes: review queue (WHERE next_review_at <= now)
CREATE INDEX idx_notes_next_review ON notes(next_review_at);

-- Note tags: lookup by note, and filter by category+value
CREATE INDEX idx_note_tags_note ON note_tags(note_id);
CREATE INDEX idx_note_tags_category_value ON note_tags(tag_category, tag_value);

-- Note relations: lookup by source and target
CREATE INDEX idx_note_relations_source ON note_relations(source_note_id);
CREATE INDEX idx_note_relations_target ON note_relations(target_note_id);

-- Chat sessions: query by user + language, and filter by status
CREATE INDEX idx_chat_sessions_user_language ON chat_sessions(user_id, learning_language_code);
CREATE INDEX idx_chat_sessions_status ON chat_sessions(status);

-- Recent messages: context loading by user + learning language
CREATE INDEX idx_recent_messages_user_learning_language ON recent_messages(user_id, learning_language_code);

-- Recent messages: lookup by session
CREATE INDEX idx_recent_messages_session ON recent_messages(session_id);

-- Recent messages: TTL cleanup job (DELETE WHERE expires_at < now)
CREATE INDEX idx_recent_messages_expires ON recent_messages(expires_at);

-- Conversation summaries: context loading ordered by time
CREATE INDEX idx_summaries_user_learning_language_time ON conversation_summaries(user_id, learning_language_code, created_at);

-- Review events: analytics queries by user + time
CREATE INDEX idx_review_events_user_time ON review_events(user_id, reviewed_at);

