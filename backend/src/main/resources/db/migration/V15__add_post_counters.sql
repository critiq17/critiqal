ALTER TABLE posts
    ADD COLUMN like_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN comment_count INTEGER NOT NULL DEFAULT 0;

UPDATE posts p
SET like_count = COALESCE((SELECT COUNT(*)
                           FROM post_likes pl
                           WHERE pl.post_id = p.id
    ), 0);

UPDATE posts p
SET comment_count = COALESCE((
    SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id
    ), 0);