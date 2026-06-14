CREATE TABLE user_activity_stats (
    user_id UUID NOT NULL,
    posts_count BIGINT NOT NULL DEFAULT 0,
    comments_count BIGINT NOT NULL DEFAULT 0,
    likes_count BIGINT NOT NULL DEFAULT 0,
    member_days INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_activity_stats PRIMARY KEY (user_id),
    CONSTRAINT fk_uas_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_uas_non_negative
        CHECK (posts_count >= 0 AND comments_count >= 0
               AND likes_count >= 0 AND member_days >= 0)
);


INSERT INTO user_activity_stats (user_id, posts_count, comments_count, likes_count, member_days)
SELECT
    u.id,
    COALESCE((SELECT COUNT(*) FROM posts p WHERE p.author_id = u.id AND p.status = 'PUBLISHED'), 0),
    COALESCE((SELECT COUNT(*) FROM comments c WHERE c.author_id = u.id), 0),
    COALESCE((SELECT COUNT(*) FROM post_likes pl WHERE pl.user_id = u.id), 0),
    GREATEST(0, EXTRACT(DAY FROM CURRENT_TIMESTAMP - u.created_at)::INT)
FROM users u
ON CONFLICT (user_id) DO NOTHING;

CREATE INDEX idx_uas_members_day ON user_activity_stats (member_days);