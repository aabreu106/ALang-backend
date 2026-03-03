ALTER TABLE review_events DROP CONSTRAINT review_events_note_id_fkey;
ALTER TABLE review_events ADD CONSTRAINT review_events_note_id_fkey
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE;
