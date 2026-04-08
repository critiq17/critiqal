package org.acme.api.dtos;

import org.acme.domain.post.Post;
import org.acme.domain.post.PostStatus;

import java.time.Instant;

public record PostDTO(
        Long id,
        UserDTO author,
        String content,
        String photoUrl,
        String photoThumbnailUrl,
        long viewCount,
        PostStatus status,
        Instant createdAt
) {
    public static PostDTO from(Post post) {
        return new PostDTO(
                post.id,
                UserDTO.from(post.author),
                post.content,
                post.photoUrl,
                post.photoThumbnailUrl,
                post.viewCount,
                post.status,
                post.createdAt
        );
    }
}