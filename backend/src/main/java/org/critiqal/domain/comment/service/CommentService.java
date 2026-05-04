package org.critiqal.domain.comment.service;

import org.critiqal.domain.comment.Comment;

import java.util.List;

public interface CommentService {

    List<Comment> getPostComments(Long postId);

    List<Comment> getRootComments(Long postId);

    List<Comment> getReplies(Long postId, Long commentId);

    Comment addComment(Long authorId, Long postId, String content);

    Comment addReply(Long authorId, Long postId, Long parentId, String content);

    void deleteComment(Long postId, Long commentId, Long requestedId);
}
