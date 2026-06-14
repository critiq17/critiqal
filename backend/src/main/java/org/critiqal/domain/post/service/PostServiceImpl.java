package org.critiqal.domain.post.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.critiqal.domain.activity.ActivityEvent;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.post.PostCreatedEvent;
import org.critiqal.domain.post.PostStatus;
import org.critiqal.domain.post.repository.PostRepository;
import org.critiqal.domain.post_view.PostView;
import org.critiqal.domain.post_view.PostViewId;
import org.critiqal.domain.post_view.repository.PostViewRepository;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.user.service.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link PostService}.
 * Applies post domain rules and assembles feed query results.
 */
@ApplicationScoped
public class PostServiceImpl implements PostService {

    private final PostRepository postRepo;
    private final PostViewRepository postViewRepo;
    private final UserService userService;
    private final Event<PostCreatedEvent> postCreatedEvent;
    private final Event<ActivityEvent> activityEvent;

    public PostServiceImpl(PostRepository postRepo,
                           PostViewRepository postViewRepo,
                           UserService userService,
                           Event<PostCreatedEvent> postCreatedEvent,
                           Event<ActivityEvent> activityEvent) {
        this.postRepo = postRepo;
        this.postViewRepo = postViewRepo;
        this.userService = userService;
        this.postCreatedEvent = postCreatedEvent;
        this.activityEvent = activityEvent;
    }

    @Override
    @Transactional
    public Post createPost(UUID authorId, String content) {
        if (content == null || content.isBlank()) {
            throw new DomainException("Post cannot be empty");
        }
        String normalized = normalizeContent(content);
        if (normalized.isBlank()) {
            throw new DomainException("Post cannot be empty");
        }
        if (normalized.length() > 500) {
            throw new DomainException("Post cannot exceed 500 characters");
        }
        var author = userService.getById(authorId);
        var post = new Post();
        post.author = author;
        post.content = normalized;

        post = postRepo.save(post);
        postCreatedEvent.fireAsync(new PostCreatedEvent(post.id, author.id));
        activityEvent.fireAsync(new ActivityEvent(authorId, ActivityEvent.ActivityType.POST_CREATED));
        return post;
    }

    private static String normalizeContent(String raw) {
        // 1. Normalize line endings
        String s = raw.replace("\r\n", "\n").replace("\r", "\n");
        // 2. Strip trailing whitespace from each line
        String[] lines = s.split("\n", -1);
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i].stripTrailing());
            if (i < lines.length - 1) sb.append('\n');
        }
        // 3. Collapse 3+ consecutive newlines → max 2
        String result = sb.toString().replaceAll("\n{3,}", "\n\n");
        // 4. Strip leading/trailing
        return result.strip();
    }

    @Override
    public Page<Post> getUserPost(UUID authorId, int page, int size) {
        var ids = postRepo.findByAuthorIds(authorId, page, size);
        var posts = postRepo.findByIdsWithRelations(ids);
        var total = postRepo.countByAuthor(authorId);
        var postsById = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        var ordered = ids.stream().map(postsById::get).filter(Objects::nonNull).toList();
        return Page.of(ordered, page, size, total);
    }

    @Override
    public long countByAuthor(UUID authorId) {
        return postRepo.countByAuthor(authorId);
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
    public Post getById(UUID postId) {
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
    public Page<Post> getFollowingFeed(UUID userId, int page, int size) {
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
    public void view(UUID postId, UUID userId) {
        if (userId == null) {
            return;
        }

        var viewId = new PostViewId(postId, userId);
        var existing = postViewRepo.findByIdOptional(viewId);

        if (existing.isEmpty()) {
            var view = new PostView();
            view.id = viewId;
            postViewRepo.save(view);
            postRepo.incrementViews(postId);
            return;
        }

        var view = existing.get();
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        if (view.lastViewedAt.isBefore(oneHourAgo)) {
            view.lastViewedAt = Instant.now();
            postRepo.incrementViews(postId);
        }
    }

    @Override
    @Transactional
    public void deletePost(UUID postId, UUID requestedId) {
        var post = getById(postId);
        if (!post.author.id.equals(requestedId)) {
            throw new ForbiddenException("Not your post");
        }
        post.status = PostStatus.DELETED;
    }

    @Override
    @Transactional
    public void view(UUID postId) {
        postRepo.incrementViews(postId);
    }

    @Override
    @Transactional
    public void recountPost(UUID postId) {
        getById(postId);
        postRepo.recount(postId);
    }
}
