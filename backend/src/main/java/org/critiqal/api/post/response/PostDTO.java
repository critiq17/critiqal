package org.critiqal.api.post.response;

import org.critiqal.api.user.response.UserDTO;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.post.PostStatus;
import org.critiqal.domain.post_photo.PostPhoto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostDTO(
        UUID id,
        UserDTO author,
        String content,
        List<PostPhotoDTO> photos,
        long viewCount,
        long likeCount,
        long commentCount,
        boolean likedByMe,
        PostStatus status,
        Instant createdAt
) {
    public record PostPhotoDTO(UUID id, String url, int position) {
        public static PostPhotoDTO from(PostPhoto photo) {
            return new PostPhotoDTO(photo.id, photo.url, photo.position);
        }
    }

    public static PostDTO from(Post post, long likeCount, boolean likedByMe) {
        var photos = post.photos != null
                ? post.photos.stream().map(PostPhotoDTO::from).toList()
                : List.<PostPhotoDTO>of();
        return new PostDTO(
                post.id,
                UserDTO.from(post.author),
                post.content,
                photos,
                post.viewCount,
                likeCount,
                post.commentCount,
                likedByMe,
                post.status,
                post.createdAt
        );
    }
}
