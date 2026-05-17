-- Add self-referential parent_id to comments for threaded replies.
-- parent_id IS NULL  → root comment
-- parent_id IS NOT NULL → reply (max one level deep, enforced in CommentService)
--
-- ON DELETE CASCADE: when a parent comment is deleted, all its replies are
-- removed automatically at the DB level, consistent with CascadeType.ALL on
-- the JPA side and the single-level depth constraint.

ALTER TABLE comments
    ADD COLUMN parent_id BIGINT NULL,
    ADD CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE;

-- Supports CommentRepository.findReplies("parent.id = ?1 ORDER BY createdAt ASC")
CREATE INDEX idx_comments_parent_id ON comments (parent_id, created_at ASC);
