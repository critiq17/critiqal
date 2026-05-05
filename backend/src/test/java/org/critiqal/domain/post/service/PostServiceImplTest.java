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
        assertThrows(DomainException.class, () -> service.createPost(1L, "  "));
        verifyNoInteractions(userService, postRepo, postCreatedEvent);
    }

    @Test
    void createPostSavesPostAndFiresEvent() {
        var author = new User();
        author.id = 7L;

        when(userService.getById(7L)).thenReturn(author);
        when(postRepo.save(any(Post.class))).thenAnswer(invocation -> {
            var post = invocation.getArgument(0, Post.class);
            post.id = 11L;
            return post;
        });

        var result = service.createPost(7L, "hello world");

        assertEquals(11L, result.id);
        assertSame(author, result.author);
        assertEquals("hello world", result.content);
        verify(postRepo).save(argThat(post -> post.author == author && "hello world".equals(post.content)));
        verify(postCreatedEvent).fireAsync(argThat(event -> event.postId().equals(11L) && event.authorId().equals(7L)));
    }

    @Test
    void getUserPostOrdersContentByRequestedIds() {
        var post1 = postWithId(1L);
        var post3 = postWithId(3L);

        when(postRepo.findByAuthorIds(42L, 1, 2)).thenReturn(List.of(3L, 2L, 1L));
        when(postRepo.findByIdsWithRelations(List.of(3L, 2L, 1L))).thenReturn(List.of(post1, post3));
        when(postRepo.countByAuthor(42L)).thenReturn(5L);

        var page = service.getUserPost(42L, 1, 2);

        assertEquals(List.of(post3, post1), page.content());
        assertEquals(5L, page.total());
        assertTrue(page.hasNext());
    }

    @Test
    void getLatestFeedOrdersContentByRequestedIds() {
        var post2 = postWithId(2L);
        var post1 = postWithId(1L);

        when(postRepo.findLatestIds(0, 2)).thenReturn(List.of(1L, 2L));
        when(postRepo.findByIdsWithRelations(List.of(1L, 2L))).thenReturn(List.of(post2, post1));
        when(postRepo.countPublished()).thenReturn(2L);

        var page = service.getLatestFeed(0, 2);

        assertEquals(List.of(post1, post2), page.content());
        assertFalse(page.hasNext());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(postRepo.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(99L));
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
        var post1 = postWithId(1L);
        var post3 = postWithId(3L);

        when(postRepo.searchIds("run", 0, 10)).thenReturn(List.of(3L, 1L));
        when(postRepo.findByIdsWithRelations(List.of(3L, 1L))).thenReturn(List.of(post1, post3));
        when(postRepo.countSearch("run")).thenReturn(2L);

        var page = service.search("run", 0, 10);

        assertEquals(List.of(post3, post1), page.content());
        assertEquals(2L, page.total());
    }

    @Test
    void getFollowingFeedShortCircuitsWhenNoIds() {
        when(postRepo.findFollowingFeedIds(5L, 0, 10)).thenReturn(List.of());

        var page = service.getFollowingFeed(5L, 0, 10);

        assertTrue(page.content().isEmpty());
        assertEquals(0L, page.total());
        verify(postRepo, never()).findByIdsWithRelations(any());
        verify(postRepo, never()).countFollowingFeed(any());
    }

    @Test
    void getFollowingFeedOrdersResultsByIdList() {
        var post2 = postWithId(2L);
        var post4 = postWithId(4L);

        when(postRepo.findFollowingFeedIds(5L, 0, 10)).thenReturn(List.of(4L, 2L));
        when(postRepo.findByIdsWithRelations(List.of(4L, 2L))).thenReturn(List.of(post2, post4));
        when(postRepo.countFollowingFeed(5L)).thenReturn(7L);

        var page = service.getFollowingFeed(5L, 0, 10);

        assertEquals(List.of(post4, post2), page.content());
        assertEquals(7L, page.total());
    }

    @Test
    void viewWithNullUserDoesNothing() {
        service.view(44L, null);

        verifyNoInteractions(postViewRepo, postRepo);
    }

    @Test
    void viewCreatesNewPostViewAndIncrementsCounterOnFirstVisit() {
        when(postViewRepo.findByIdOptional(any(PostViewId.class))).thenReturn(Optional.empty());

        service.view(11L, 22L);

        var idCaptor = ArgumentCaptor.forClass(PostView.class);
        verify(postViewRepo).save(idCaptor.capture());
        assertEquals(11L, idCaptor.getValue().id.postId);
        assertEquals(22L, idCaptor.getValue().id.userId);
        verify(postRepo).incrementViews(11L);
    }

    @Test
    void viewDoesNotIncrementForRecentVisit() {
        var view = new PostView();
        view.id = new PostViewId(11L, 22L);
        var original = Instant.now().minus(30, ChronoUnit.MINUTES);
        view.lastViewedAt = original;

        when(postViewRepo.findByIdOptional(any(PostViewId.class))).thenReturn(Optional.of(view));

        service.view(11L, 22L);

        verify(postRepo, never()).incrementViews(any());
        assertEquals(original, view.lastViewedAt);
    }

    @Test
    void viewRefreshesStaleVisitAndIncrementsCounter() {
        var view = new PostView();
        view.id = new PostViewId(11L, 22L);
        var original = Instant.now().minus(2, ChronoUnit.HOURS);
        view.lastViewedAt = original;

        when(postViewRepo.findByIdOptional(any(PostViewId.class))).thenReturn(Optional.of(view));

        service.view(11L, 22L);

        verify(postRepo).incrementViews(11L);
        assertTrue(view.lastViewedAt.isAfter(original));
    }

    @Test
    void deletePostRejectsNonOwner() {
        var author = new User();
        author.id = 2L;
        var post = new Post();
        post.author = author;

        when(postRepo.findByIdOptional(1L)).thenReturn(Optional.of(post));

        assertThrows(ForbiddenException.class, () -> service.deletePost(1L, 99L));
    }

    @Test
    void deletePostMarksOwnedPostAsDeleted() {
        var author = new User();
        author.id = 1L;
        var post = new Post();
        post.author = author;
        post.status = PostStatus.PUBLISHED;

        when(postRepo.findByIdOptional(1L)).thenReturn(Optional.of(post));

        service.deletePost(1L, 1L);

        assertEquals(PostStatus.DELETED, post.status);
    }

    @Test
    void anonymousViewDelegatesToRepositoryCounter() {
        service.view(55L);

        verify(postRepo).incrementViews(55L);
    }

    private static Post postWithId(Long id) {
        var post = new Post();
        post.id = id;
        return post;
    }
}
