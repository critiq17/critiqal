package org.critiqal.api.dtos;

import org.critiqal.domain.comment.Comment;

import java.time.Instant;

public record CommentDTO(
        Long id,
        Long postId,
        UserDTO author,
        String content,
        Instant createdAt
) {
    public static CommentDTO from(Comment comment) {
        return new CommentDTO(
                comment.id,
                comment.post.id,
                UserDTO.from(comment.author),
                comment.content,
                comment.createdAt
        );
    }
}
