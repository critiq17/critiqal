CREATE EXTENSION IF NOT EXISTS "pgcrypto";

ALTER TABLE users ADD COLUMN id_uuid UUID;
UPDATE users SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;

ALTER TABLE posts ADD COLUMN id_uuid UUID;
ALTER TABLE posts ADD COLUMN author_id_uuid UUID;
UPDATE posts SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;
UPDATE posts p
SET author_id_uuid = u.id_uuid
FROM users u
WHERE p.author_id = u.id;

ALTER TABLE comments ADD COLUMN id_uuid UUID;
ALTER TABLE comments ADD COLUMN post_id_uuid UUID;
ALTER TABLE comments ADD COLUMN author_id_uuid UUID;
ALTER TABLE comments ADD COLUMN parent_id_uuid UUID;
UPDATE comments SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;
UPDATE comments c
SET post_id_uuid = p.id_uuid
FROM posts p
WHERE c.post_id = p.id;
UPDATE comments c
SET author_id_uuid = u.id_uuid
FROM users u
WHERE c.author_id = u.id;
UPDATE comments c
SET parent_id_uuid = parent.id_uuid
FROM comments parent
WHERE c.parent_id = parent.id;

ALTER TABLE reactions ADD COLUMN id_uuid UUID;
ALTER TABLE reactions ADD COLUMN post_id_uuid UUID;
ALTER TABLE reactions ADD COLUMN user_id_uuid UUID;
UPDATE reactions SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;
UPDATE reactions r
SET post_id_uuid = p.id_uuid
FROM posts p
WHERE r.post_id = p.id;
UPDATE reactions r
SET user_id_uuid = u.id_uuid
FROM users u
WHERE r.user_id = u.id;

ALTER TABLE follows ADD COLUMN id_uuid UUID;
ALTER TABLE follows ADD COLUMN follower_id_uuid UUID;
ALTER TABLE follows ADD COLUMN following_id_uuid UUID;
UPDATE follows SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;
UPDATE follows f
SET follower_id_uuid = follower.id_uuid
FROM users follower
WHERE f.follower_id = follower.id;
UPDATE follows f
SET following_id_uuid = following_user.id_uuid
FROM users following_user
WHERE f.following_id = following_user.id;

ALTER TABLE post_photos ADD COLUMN id_uuid UUID;
ALTER TABLE post_photos ADD COLUMN post_id_uuid UUID;
UPDATE post_photos SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;
UPDATE post_photos pp
SET post_id_uuid = p.id_uuid
FROM posts p
WHERE pp.post_id = p.id;

ALTER TABLE post_views ADD COLUMN post_id_uuid UUID;
ALTER TABLE post_views ADD COLUMN user_id_uuid UUID;
UPDATE post_views pv
SET post_id_uuid = p.id_uuid
FROM posts p
WHERE pv.post_id = p.id;
UPDATE post_views pv
SET user_id_uuid = u.id_uuid
FROM users u
WHERE pv.user_id = u.id;

ALTER TABLE strava_integrations ADD COLUMN id_uuid UUID;
ALTER TABLE strava_integrations ADD COLUMN user_id_uuid UUID;
UPDATE strava_integrations SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;
UPDATE strava_integrations si
SET user_id_uuid = u.id_uuid
FROM users u
WHERE si.user_id = u.id;

ALTER TABLE post_views
    DROP CONSTRAINT IF EXISTS fk_post_views_post,
    DROP CONSTRAINT IF EXISTS fk_post_views_user,
    DROP CONSTRAINT IF EXISTS pk_post_views;

ALTER TABLE post_photos
    DROP CONSTRAINT IF EXISTS fk_post_photos_post,
    DROP CONSTRAINT IF EXISTS pk_post_photos;

ALTER TABLE reactions
    DROP CONSTRAINT IF EXISTS fk_reactions_post,
    DROP CONSTRAINT IF EXISTS fk_reactions_user,
    DROP CONSTRAINT IF EXISTS uk_reactions_post_user,
    DROP CONSTRAINT IF EXISTS pk_reactions;

ALTER TABLE comments
    DROP CONSTRAINT IF EXISTS fk_comments_post,
    DROP CONSTRAINT IF EXISTS fk_comments_author,
    DROP CONSTRAINT IF EXISTS fk_comments_parent,
    DROP CONSTRAINT IF EXISTS pk_comments;

ALTER TABLE follows
    DROP CONSTRAINT IF EXISTS fk_follows_follower,
    DROP CONSTRAINT IF EXISTS fk_follows_following,
    DROP CONSTRAINT IF EXISTS uk_follows_follower_following,
    DROP CONSTRAINT IF EXISTS chk_follows_not_self,
    DROP CONSTRAINT IF EXISTS pk_follows;

ALTER TABLE posts
    DROP CONSTRAINT IF EXISTS fk_posts_author,
    DROP CONSTRAINT IF EXISTS pk_posts;

ALTER TABLE strava_integrations
    DROP CONSTRAINT IF EXISTS fk_strava_user,
    DROP CONSTRAINT IF EXISTS uk_strava_user,
    DROP CONSTRAINT IF EXISTS strava_integrations_pkey;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS pk_users;

ALTER TABLE post_views
    DROP COLUMN post_id,
    DROP COLUMN user_id;

ALTER TABLE post_photos
    DROP COLUMN id,
    DROP COLUMN post_id;

ALTER TABLE reactions
    DROP COLUMN id,
    DROP COLUMN post_id,
    DROP COLUMN user_id;

ALTER TABLE comments
    DROP COLUMN id,
    DROP COLUMN post_id,
    DROP COLUMN author_id,
    DROP COLUMN parent_id;

ALTER TABLE follows
    DROP COLUMN id,
    DROP COLUMN follower_id,
    DROP COLUMN following_id;

ALTER TABLE posts
    DROP COLUMN id,
    DROP COLUMN author_id;

ALTER TABLE strava_integrations
    DROP COLUMN id,
    DROP COLUMN user_id;

ALTER TABLE users
    DROP COLUMN id;

ALTER TABLE users RENAME COLUMN id_uuid TO id;

ALTER TABLE posts RENAME COLUMN id_uuid TO id;
ALTER TABLE posts RENAME COLUMN author_id_uuid TO author_id;

ALTER TABLE comments RENAME COLUMN id_uuid TO id;
ALTER TABLE comments RENAME COLUMN post_id_uuid TO post_id;
ALTER TABLE comments RENAME COLUMN author_id_uuid TO author_id;
ALTER TABLE comments RENAME COLUMN parent_id_uuid TO parent_id;

ALTER TABLE reactions RENAME COLUMN id_uuid TO id;
ALTER TABLE reactions RENAME COLUMN post_id_uuid TO post_id;
ALTER TABLE reactions RENAME COLUMN user_id_uuid TO user_id;

ALTER TABLE follows RENAME COLUMN id_uuid TO id;
ALTER TABLE follows RENAME COLUMN follower_id_uuid TO follower_id;
ALTER TABLE follows RENAME COLUMN following_id_uuid TO following_id;

ALTER TABLE post_photos RENAME COLUMN id_uuid TO id;
ALTER TABLE post_photos RENAME COLUMN post_id_uuid TO post_id;

ALTER TABLE post_views RENAME COLUMN post_id_uuid TO post_id;
ALTER TABLE post_views RENAME COLUMN user_id_uuid TO user_id;

ALTER TABLE strava_integrations RENAME COLUMN id_uuid TO id;
ALTER TABLE strava_integrations RENAME COLUMN user_id_uuid TO user_id;

ALTER TABLE users
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ADD CONSTRAINT pk_users PRIMARY KEY (id);

ALTER TABLE posts
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN author_id SET NOT NULL,
    ADD CONSTRAINT pk_posts PRIMARY KEY (id),
    ADD CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE comments
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN post_id SET NOT NULL,
    ALTER COLUMN author_id SET NOT NULL,
    ADD CONSTRAINT pk_comments PRIMARY KEY (id),
    ADD CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE;

ALTER TABLE reactions
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN post_id SET NOT NULL,
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT pk_reactions PRIMARY KEY (id),
    ADD CONSTRAINT uk_reactions_post_user UNIQUE (post_id, user_id),
    ADD CONSTRAINT fk_reactions_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE follows
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN follower_id SET NOT NULL,
    ALTER COLUMN following_id SET NOT NULL,
    ADD CONSTRAINT pk_follows PRIMARY KEY (id),
    ADD CONSTRAINT uk_follows_follower_following UNIQUE (follower_id, following_id),
    ADD CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_follows_not_self CHECK (follower_id <> following_id);

ALTER TABLE post_photos
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN post_id SET NOT NULL,
    ADD CONSTRAINT pk_post_photos PRIMARY KEY (id),
    ADD CONSTRAINT fk_post_photos_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE;

ALTER TABLE post_views
    ALTER COLUMN post_id SET NOT NULL,
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT pk_post_views PRIMARY KEY (post_id, user_id),
    ADD CONSTRAINT fk_post_views_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_post_views_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE strava_integrations
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT pk_strava_integrations PRIMARY KEY (id),
    ADD CONSTRAINT uk_strava_user UNIQUE (user_id),
    ADD CONSTRAINT fk_strava_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_posts_author_status_created_at ON posts (author_id, status, created_at DESC);
CREATE INDEX idx_comments_post_created_at ON comments (post_id, created_at DESC);
CREATE INDEX idx_comments_author_id ON comments (author_id);
CREATE INDEX idx_comments_parent_id ON comments (parent_id, created_at ASC);
CREATE INDEX idx_reactions_post_id ON reactions (post_id);
CREATE INDEX idx_reactions_user_id ON reactions (user_id);
CREATE INDEX idx_follows_follower_id ON follows (follower_id);
CREATE INDEX idx_follows_following_id ON follows (following_id);
CREATE INDEX idx_post_photos_post_id_position ON post_photos (post_id, position ASC);
CREATE INDEX idx_strava_user_id ON strava_integrations (user_id);

DROP SEQUENCE IF EXISTS users_seq;
DROP SEQUENCE IF EXISTS posts_seq;
DROP SEQUENCE IF EXISTS comments_seq;
DROP SEQUENCE IF EXISTS reactions_seq;
DROP SEQUENCE IF EXISTS follows_seq;
DROP SEQUENCE IF EXISTS post_photos_seq;
DROP SEQUENCE IF EXISTS strava_integrations_id_seq;
