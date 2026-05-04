package org.critiqal.domain.post.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.post.PostCreatedEvent;
import org.critiqal.domain.post.PostStatus;
import org.critiqal.domain.post.repository.PostRepository;
import org.critiqal.domain.post_view.PostView;
import org.critiqal.domain.post_view.PostViewId;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.user.service.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostServiceImpl implements PostService {

    private final PostRepository postRepo;
    private final UserService userService;
    private final Event<PostCreatedEvent> postCreatedEvent;

    public PostServiceImpl(PostRepository postRepo,
                           UserService userService,
                           Event<PostCreatedEvent> postCreatedEvent) {
        this.postRepo = postRepo;
        this.userService = userService;
        this.postCreatedEvent = postCreatedEvent;
    }

    @Override
    @Transactional
    public Post createPost(Long authorId, String content) {
        if (content == null || content.isBlank()) {
            throw new DomainException("Post cannot be empty");
        }

        var author = userService.getById(authorId);
        var post = new Post();
        post.author = author;
        post.content = content;

        post = postRepo.save(post);
        postCreatedEvent.fireAsync(new PostCreatedEvent(post.id, author.id));
        return post;
    }

    @Override
    public Page<Post> getUserPost(Long authorId, int page, int size) {
        var ids = postRepo.findByAuthorIds(authorId, page, size);
        var posts = postRepo.findByIdsWithRelations(ids);
        var total = postRepo.countByAuthor(authorId);
        var postsById = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        var ordered = ids.stream().map(postsById::get).filter(Objects::nonNull).toList();
        return Page.of(ordered, page, size, total);
    }

    @Override
    public Page<Post> getLatestFeed(int page, int size) {
        var ids = postRepo.findLatestIds(page, size);
        var posts = postRepo.findByIdsWithRelations(ids);
        var total = postRepo.countPublished();
        var postsById = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        var ordered = ids.stream().map(postsById::get).filter(Objects::nonNull).toList();
        return Page.of(ordered, page, size, total);
    }

    @Override
    public Post getById(Long postId) {
        return postRepo.findByIdOptional(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
    }

    @Override
    public Page<Post> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return Page.of(List.of(), page, size, 0);
        }

        var ids = postRepo.searchIds(query, page, size);
        var posts = postRepo.findByIdsWithRelations(ids);
        var total = postRepo.countSearch(query);
        var postsById = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        var orderedPosts = ids.stream().map(postsById::get).filter(Objects::nonNull).toList();
        return Page.of(orderedPosts, page, size, total);
    }

    @Override
    public Page<Post> getFollowingFeed(Long userId, int page, int size) {
        var ids = postRepo.findFollowingFeedIds(userId, page, size);
        if (ids.isEmpty()) {
            return Page.of(List.of(), page, size, 0);
        }

        var posts = postRepo.findByIdsWithRelations(ids);
        var total = postRepo.countFollowingFeed(userId);
        var postsById = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        var ordered = ids.stream().map(postsById::get).filter(Objects::nonNull).toList();
        return Page.of(ordered, page, size, total);
    }

    @Override
    @Transactional
    public void view(Long postId, Long userId) {
        if (userId == null) {
            return;
        }

        var viewId = new PostViewId(postId, userId);
        var existing = PostView.findById(viewId);

        if (existing == null) {
            var view = new PostView();
            view.id = viewId;
            view.persist();
            postRepo.incrementViews(postId);
            return;
        }

        var view = (PostView) existing;
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        if (view.lastViewedAt.isBefore(oneHourAgo)) {
            view.lastViewedAt = Instant.now();
            postRepo.incrementViews(postId);
        }
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long requestedId) {
        var post = getById(postId);
        if (!post.author.id.equals(requestedId)) {
            throw new ForbiddenException("Not your post");
        }
        post.status = PostStatus.DELETED;
    }

    @Override
    @Transactional
    public void view(Long postId) {
        postRepo.incrementViews(postId);
    }
}
