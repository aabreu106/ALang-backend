-- Add note_id to chat_sessions: tracks the note created from this session
ALTER TABLE chat_sessions
    ADD COLUMN note_id VARCHAR(255) REFERENCES notes(id);
