package org.critiqal.domain.comment.repository;

import org.critiqal.domain.comment.Comment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines persistence operations for comments.
 * Supports post lookups, replies, storage, and deletion.
 */
public interface CommentRepository {

    List<Comment> findByPost(UUID postId);

    List<Comment> findByRootPost(UUID postId);

    List<Comment> findReplies(UUID parentId);

    Optional<Comment> findByIdOptional(UUID commentId);

    Comment save(Comment comment);

    void delete(Comment comment);
}
