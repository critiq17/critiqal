package org.critiqal.api.post;

import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.post.response.PostDTO;
import org.critiqal.domain.like.service.PostLikeServiceImpl;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.shared.pagination.Page;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class PostEnricher {

    private final CurrentUser currentUser;
    private final PostLikeServiceImpl postLikeService;

    public PostEnricher(CurrentUser currentUser, PostLikeServiceImpl postLikeService) {
        this.currentUser = currentUser;
        this.postLikeService = postLikeService;
    }

    public Page<PostDTO> enrichWithLikes(Page<Post> page) {
        if (page.content().isEmpty()) {
            return page.map(post -> PostDTO.from(post, 0L, false));
        }

        var ids = page.content().stream().map(Post::getId).toList();

        UUID userId = currentUser.idOrNull();
        Set<UUID> liked = userId != null
                ? postLikeService.likedPostIds(userId, ids)
                : Set.of();

        return page.map(post -> PostDTO.from(
                post,
                post.likeCount,
                liked.contains(post.id)
        ));
    }
}
