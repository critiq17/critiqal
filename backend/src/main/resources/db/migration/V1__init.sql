-- Base schema for the current JPA/Panache model.
-- Names follow Quarkus/Hibernate camelCase to snake_case mapping.

-- Sequences for PanacheEntity generated ids.
CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS posts_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS comments_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS reactions_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS follows_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE users (
    id BIGINT NOT NULL DEFAULT nextval('users_seq'),
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    bio VARCHAR(255),
    avatar_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE posts (
    id BIGINT NOT NULL DEFAULT nextval('posts_seq'),
    author_id BIGINT NOT NULL,
    content TEXT,
    photo_url VARCHAR(255),
    photo_thumbnail_url VARCHAR(255),
    view_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_posts PRIMARY KEY (id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_posts_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'DELETED', 'ARCHIVED'))
);

CREATE TABLE comments (
    id BIGINT NOT NULL DEFAULT nextval('comments_seq'),
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE reactions (
    id BIGINT NOT NULL DEFAULT nextval('reactions_seq'),
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    CONSTRAINT pk_reactions PRIMARY KEY (id),
    CONSTRAINT uk_reactions_post_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_reactions_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_reactions_type CHECK (type IN ('GIGACHAD', 'THE_ROCK', 'DAVID'))
);

CREATE TABLE follows (
    id BIGINT NOT NULL DEFAULT nextval('follows_seq'),
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_follows PRIMARY KEY (id),
    CONSTRAINT uk_follows_follower_following UNIQUE (follower_id, following_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> following_id)
);

-- Indexes for the main repository queries.
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);
CREATE INDEX idx_posts_status_created_at ON posts (status, created_at DESC);
CREATE INDEX idx_posts_author_status_created_at ON posts (author_id, status, created_at DESC);

CREATE INDEX idx_comments_post_created_at ON comments (post_id, created_at DESC);
CREATE INDEX idx_comments_author_id ON comments (author_id);

CREATE INDEX idx_reactions_post_id ON reactions (post_id);
CREATE INDEX idx_reactions_user_id ON reactions (user_id);

CREATE INDEX idx_follows_follower_id ON follows (follower_id);
CREATE INDEX idx_follows_following_id ON follows (following_id);
