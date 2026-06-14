ALTER TABLE badges
    ADD COLUMN IF NOT EXISTS config JSONB;

UPDATE badges SET config = '{"max_members": 100}' WHERE code = 'ORIGIN';

UPDATE badges SET config = '{"trigger": "first post"}' WHERE code = 'SCRIBE';
UPDATE badges SET config = '{"min_posts": 50}' WHERE code = 'ORATOR';
UPDATE badges SET config = '{"min_days": 100}' WHERE code = 'CENTURION';
UPDATE badges SET config = '{"min_days": 365}' WHERE code = 'GLADIATOR';
UPDATE badges SET config = '{"min_days": 730}' WHERE code = 'LEGATUS';
UPDATE badges SET config = '{"min_posts": 500, "min_comments": 100, "min_likes": 1000}' WHERE code = 'TRIBUNE';