package org.critiqal.api.post.response;

import org.critiqal.api.user.response.UserDTO;
import org.critiqal.domain.comment.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentDTO(
        UUID id,
        UUID postId,
        UserDTO author,
        String content,
        long likeCount,
        boolean likedByMe,
        Instant createdAt
) {
    public static CommentDTO from(Comment comment, long likeCount, boolean likedByMe) {
        return new CommentDTO(
                comment.id,
                comment.post.id,
                UserDTO.from(comment.author),   
                comment.content,
                likeCount,
                likedByMe,
                comment.createdAt
        );
    }
}
