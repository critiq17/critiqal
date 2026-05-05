package org.critiqal.domain.comment.repository;

import org.critiqal.domain.comment.Comment;

import java.util.List;
import java.util.Optional;

/**
 * Defines persistence operations for comments.
 * Supports post lookups, replies, storage, and deletion.
 */
public interface CommentRepository {

    List<Comment> findByPost(Long postId);

    List<Comment> findByRootPost(Long postId);

    List<Comment> findReplies(Long parentId);

    Optional<Comment> findByIdOptional(Long commentId);

    Comment save(Comment comment);

    void delete(Comment comment);
}
