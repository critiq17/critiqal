package org.acme.domain.post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.api.dtos.PageResponse;
import org.acme.domain.post_view.PostView;
import org.acme.domain.post_view.PostViewId;
import org.acme.domain.user.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostService {

    @Inject PostRepository postRepo;
    @Inject
    UserService userService;

    @Inject
    Event<PostCreatedEvent> postCreatedEvent;

    @Transactional
    public Post createPost(Long authorId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Post cannot be empty");
        }
        var author = userService.getById(authorId);
        var post = postRepo.createPost(author, content);

        postCreatedEvent.fireAsync(new PostCreatedEvent(post.id, author.id));
        return post;
    }

    public PageResponse<Post> getUserPost(Long authorId, int page, int size) {
        var posts = postRepo.findByAuthor(authorId, page, size);
        var total = postRepo.countByAuthor(authorId);
        return PageResponse.of(posts, page, size, total);
    }

    public PageResponse<Post> getLatestFeed(int page, int size) {
        var posts = postRepo.findLatest(page, size);
        var total = postRepo.countPublished();
        return PageResponse.of(posts, page, size, total);
    }

    public Post getById(Long postId) {
        return postRepo.findByIdOptional(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    public PageResponse<Post> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return PageResponse.of(List.of(), page, size, 0);
        }

        var ids = postRepo.searchIds(query, page, size);
        var posts = postRepo.findByIdsWithRelations(ids);
        var total = postRepo.countSearch(query);

        var postsById = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));
        var orderedPosts = ids.stream()
                .map(postsById::get)
                .filter(Objects::nonNull)
                .toList();
        return PageResponse.of(orderedPosts, page, size, total);
    }

    @Transactional
    public void view(Long postId, Long userId) {
        if (userId == null) return;

        var viewId = new PostViewId(postId, userId);
        var existing = PostView.findById(viewId);

        if (existing == null) {
            var view = new PostView();
            view.id = viewId;
            view.persist();
            postRepo.incrementViews(postId);
        } else {
            var view = (PostView) existing;
            var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            if (view.lastViewedAt.isBefore(oneHourAgo)) {
                view.lastViewedAt = Instant.now();
                postRepo.incrementViews(postId);
            }
        }
    }

    @Transactional
    public void deletePost(Long postId, Long requestedId) {
        var post = getById(postId);
        if (!post.author.id.equals(requestedId)) {
            throw new IllegalArgumentException("Not your post");
        }
        post.status = PostStatus.DELETED;
    }

    @Transactional
    public void view(Long postId) {
        postRepo.incrementViews(postId);
    }

}
