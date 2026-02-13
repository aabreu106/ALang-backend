-- ===========================================================================
-- V2: Seed supported languages
-- ===========================================================================

INSERT INTO languages (code, name, native_name, fully_supported, preferred_model) VALUES
    ('en', 'English',    'English',   TRUE, NULL),
    ('ja', 'Japanese',   '日本語',    TRUE, NULL),
    ('es', 'Spanish',    'Español',   TRUE, NULL),
    ('fr', 'French',     'Français',  TRUE, NULL),
    ('de', 'German',     'Deutsch',   TRUE, NULL),
    ('ko', 'Korean',     '한국어',    TRUE, NULL),
    ('zh', 'Chinese',    '中文',      TRUE, NULL),
    ('it', 'Italian',    'Italiano',  TRUE, NULL),
    ('pt', 'Portuguese', 'Português', TRUE, NULL);
