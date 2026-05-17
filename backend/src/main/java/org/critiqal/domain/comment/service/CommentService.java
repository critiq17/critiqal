package org.critiqal.domain.comment.service;

import org.critiqal.domain.comment.Comment;

import java.util.List;
import java.util.UUID;

/**
 * Defines comment management operations for posts.
 * Handles top-level comments, replies, and deletion rules.
 */
public interface CommentService {

    List<Comment> getPostComments(UUID postId);

    List<Comment> getRootComments(UUID postId);

    List<Comment> getReplies(UUID postId, UUID commentId);

    Comment addComment(UUID authorId, UUID postId, String content);

    Comment addReply(UUID authorId, UUID postId, UUID parentId, String content);

    void deleteComment(UUID postId, UUID commentId, UUID requestedId);
}
