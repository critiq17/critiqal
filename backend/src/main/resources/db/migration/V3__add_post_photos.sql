-- Migrate from single photo columns on posts to a separate post_photos table.
-- posts.photo_url / photo_thumbnail_url → post_photos (position 0)
-- Supports: PostPhotoRepository.findByPost, countByPost, deleteByPost

CREATE SEQUENCE IF NOT EXISTS post_photos_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE post_photos (
    id             BIGINT      NOT NULL DEFAULT nextval('post_photos_seq'),
    post_id        BIGINT      NOT NULL,
    url            VARCHAR(255) NOT NULL,
    thumbnail_url  VARCHAR(255),
    position       INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_post_photos PRIMARY KEY (id),
    CONSTRAINT fk_post_photos_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT chk_post_photos_position CHECK (position >= 0)
);

-- Supports PostPhotoRepository.findByPost("post.id = ?1 ORDER BY position ASC")
CREATE INDEX idx_post_photos_post_id_position ON post_photos (post_id, position ASC);

-- Migrate existing single photos into the new table at position 0.
INSERT INTO post_photos (post_id, url, thumbnail_url, position, created_at)
SELECT id, photo_url, photo_thumbnail_url, 0, created_at
FROM posts
WHERE photo_url IS NOT NULL;

-- Drop old single-photo columns — now fully replaced by post_photos.
ALTER TABLE posts
    DROP COLUMN IF EXISTS photo_url,
    DROP COLUMN IF EXISTS photo_thumbnail_url;
