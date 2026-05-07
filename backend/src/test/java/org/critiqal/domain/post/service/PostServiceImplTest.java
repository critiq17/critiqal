package org.critiqal.domain.post.service;

import jakarta.enterprise.event.Event;
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
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostServiceImplTest {

    private final PostRepository postRepo = mock(PostRepository.class);
    private final PostViewRepository postViewRepo = mock(PostViewRepository.class);
    private final UserService userService = mock(UserService.class);
    @SuppressWarnings("unchecked")
    private final Event<PostCreatedEvent> postCreatedEvent = mock(Event.class);

    private PostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PostServiceImpl(postRepo, postViewRepo, userService, postCreatedEvent);
    }

    @Test
    void createPostRejectsBlankContent() {
        assertThrows(DomainException.class, () -> service.createPost(uuid(1), "  "));
        verifyNoInteractions(userService, postRepo, postCreatedEvent);
    }

    @Test
    void createPostSavesPostAndFiresEvent() {
        var author = new User();
        author.id = uuid(7);

        when(userService.getById(uuid(7))).thenReturn(author);
        when(postRepo.save(any(Post.class))).thenAnswer(invocation -> {
            var post = invocation.getArgument(0, Post.class);
            post.id = uuid(11);
            return post;
        });

        var result = service.createPost(uuid(7), "hello world");

        assertEquals(uuid(11), result.id);
        assertSame(author, result.author);
        assertEquals("hello world", result.content);
        verify(postRepo).save(argThat(post -> post.author == author && "hello world".equals(post.content)));
        verify(postCreatedEvent).fireAsync(argThat(event -> event.postId().equals(uuid(11)) && event.authorId().equals(uuid(7))));
    }

    @Test
    void getUserPostOrdersContentByRequestedIds() {
        var post1 = postWithId(1);
        var post3 = postWithId(3);

        when(postRepo.findByAuthorIds(uuid(42), 1, 2)).thenReturn(List.of(uuid(3), uuid(2), uuid(1)));
        when(postRepo.findByIdsWithRelations(List.of(uuid(3), uuid(2), uuid(1)))).thenReturn(List.of(post1, post3));
        when(postRepo.countByAuthor(uuid(42))).thenReturn(5L);

        var page = service.getUserPost(uuid(42), 1, 2);

        assertEquals(List.of(post3, post1), page.content());
        assertEquals(5L, page.total());
        assertTrue(page.hasNext());
    }

    @Test
    void getLatestFeedOrdersContentByRequestedIds() {
        var post2 = postWithId(2);
        var post1 = postWithId(1);

        when(postRepo.findLatestIds(0, 2)).thenReturn(List.of(uuid(1), uuid(2)));
        when(postRepo.findByIdsWithRelations(List.of(uuid(1), uuid(2)))).thenReturn(List.of(post2, post1));
        when(postRepo.countPublished()).thenReturn(2L);

        var page = service.getLatestFeed(0, 2);

        assertEquals(List.of(post1, post2), page.content());
        assertFalse(page.hasNext());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(postRepo.findByIdOptional(uuid(99))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(uuid(99)));
    }

    @Test
    void searchReturnsEmptyPageForBlankQuery() {
        var page = service.search(" ", 0, 20);

        assertTrue(page.content().isEmpty());
        assertEquals(0L, page.total());
        verifyNoInteractions(postRepo);
    }

    @Test
    void searchOrdersResultsByIdList() {
        var post1 = postWithId(1);
        var post3 = postWithId(3);

        when(postRepo.searchIds("run", 0, 10)).thenReturn(List.of(uuid(3), uuid(1)));
        when(postRepo.findByIdsWithRelations(List.of(uuid(3), uuid(1)))).thenReturn(List.of(post1, post3));
        when(postRepo.countSearch("run")).thenReturn(2L);

        var page = service.search("run", 0, 10);

        assertEquals(List.of(post3, post1), page.content());
        assertEquals(2L, page.total());
    }

    @Test
    void getFollowingFeedShortCircuitsWhenNoIds() {
        when(postRepo.findFollowingFeedIds(uuid(5), 0, 10)).thenReturn(List.of());

        var page = service.getFollowingFeed(uuid(5), 0, 10);

        assertTrue(page.content().isEmpty());
        assertEquals(0L, page.total());
        verify(postRepo, never()).findByIdsWithRelations(any());
        verify(postRepo, never()).countFollowingFeed(any());
    }

    @Test
    void getFollowingFeedOrdersResultsByIdList() {
        var post2 = postWithId(2);
        var post4 = postWithId(4);

        when(postRepo.findFollowingFeedIds(uuid(5), 0, 10)).thenReturn(List.of(uuid(4), uuid(2)));
        when(postRepo.findByIdsWithRelations(List.of(uuid(4), uuid(2)))).thenReturn(List.of(post2, post4));
        when(postRepo.countFollowingFeed(uuid(5))).thenReturn(7L);

        var page = service.getFollowingFeed(uuid(5), 0, 10);

        assertEquals(List.of(post4, post2), page.content());
        assertEquals(7L, page.total());
    }

    @Test
    void viewWithNullUserDoesNothing() {
        service.view(uuid(44), null);

        verifyNoInteractions(postViewRepo, postRepo);
    }

    @Test
    void viewCreatesNewPostViewAndIncrementsCounterOnFirstVisit() {
        when(postViewRepo.findByIdOptional(any(PostViewId.class))).thenReturn(Optional.empty());

        service.view(uuid(11), uuid(22));

        var viewCaptor = ArgumentCaptor.forClass(PostView.class);
        verify(postViewRepo).save(viewCaptor.capture());
        assertEquals(uuid(11), viewCaptor.getValue().id.postId);
        assertEquals(uuid(22), viewCaptor.getValue().id.userId);
        verify(postRepo).incrementViews(uuid(11));
    }

    @Test
    void viewDoesNotIncrementForRecentVisit() {
        var view = new PostView();
        view.id = new PostViewId(uuid(11), uuid(22));
        var original = Instant.now().minus(30, ChronoUnit.MINUTES);
        view.lastViewedAt = original;

        when(postViewRepo.findByIdOptional(any(PostViewId.class))).thenReturn(Optional.of(view));

        service.view(uuid(11), uuid(22));

        verify(postRepo, never()).incrementViews(any());
        assertEquals(original, view.lastViewedAt);
    }

    @Test
    void viewRefreshesStaleVisitAndIncrementsCounter() {
        var view = new PostView();
        view.id = new PostViewId(uuid(11), uuid(22));
        var original = Instant.now().minus(2, ChronoUnit.HOURS);
        view.lastViewedAt = original;

        when(postViewRepo.findByIdOptional(any(PostViewId.class))).thenReturn(Optional.of(view));

        service.view(uuid(11), uuid(22));

        verify(postRepo).incrementViews(uuid(11));
        assertTrue(view.lastViewedAt.isAfter(original));
    }

    @Test
    void deletePostRejectsNonOwner() {
        var author = new User();
        author.id = uuid(2);
        var post = new Post();
        post.author = author;

        when(postRepo.findByIdOptional(uuid(1))).thenReturn(Optional.of(post));

        assertThrows(ForbiddenException.class, () -> service.deletePost(uuid(1), uuid(99)));
    }

    @Test
    void deletePostMarksOwnedPostAsDeleted() {
        var author = new User();
        author.id = uuid(1);
        var post = new Post();
        post.author = author;
        post.status = PostStatus.PUBLISHED;

        when(postRepo.findByIdOptional(uuid(1))).thenReturn(Optional.of(post));

        service.deletePost(uuid(1), uuid(1));

        assertEquals(PostStatus.DELETED, post.status);
    }

    @Test
    void anonymousViewDelegatesToRepositoryCounter() {
        service.view(uuid(55));

        verify(postRepo).incrementViews(uuid(55));
    }

    private static Post postWithId(long id) {
        var post = new Post();
        post.id = uuid(id);
        return post;
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
