package org.critiqal.domain.like.service;

import jakarta.enterprise.event.Event;
import org.critiqal.domain.activity.ActivityEvent;
import org.critiqal.domain.like.repository.PostLikeRepository;
import org.critiqal.domain.post.repository.PostRepository;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class PostLikeServiceImplTest {

    private final PostLikeRepository repo = mock(PostLikeRepository.class);
    private final PostService postService = mock(PostService.class);
    private final PostRepository postRepo = mock(PostRepository.class);
    @SuppressWarnings("unchecked")
    private final Event<ActivityEvent> activityEvent = mock(Event.class);

    private PostLikeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PostLikeServiceImpl(repo, postService, postRepo, activityEvent);
    }

    @Test
    void toggle_like_incrementsCounter() {
        var postId = uuid(1); var userId = uuid(2);
        when(repo.exists(postId, userId)).thenReturn(false);
        when(repo.count(postId)).thenReturn(1L);

        service.toggle(postId, userId);

        verify(postRepo).incrementLikeCount(postId);
        verify(postRepo, never()).decrementLikeCount(postId);
        verify(activityEvent).fireAsync(argThat(event ->
                event.userId().equals(userId) && event.type() == ActivityEvent.ActivityType.POST_LIKED));
    }

    @Test
    void toggle_unlike_decrementsCounter() {
        var postId = uuid(3); var userId = uuid(4);
        when(repo.exists(postId, userId)).thenReturn(true);
        when(repo.count(postId)).thenReturn(0L);

        service.toggle(postId, userId);

        verify(postRepo).decrementLikeCount(postId);
        verify(postRepo, never()).incrementLikeCount(postId);
        verify(activityEvent).fireAsync(argThat(event ->
                event.userId().equals(userId) && event.type() == ActivityEvent.ActivityType.POST_UNLIKED));
    }

    @Test
    @DisplayName("toggle adds a like when the post was not yet liked")
    void toggle_notLiked_addsLike() {
        var postId = uuid(1);
        var userId = uuid(2);
        when(repo.exists(postId, userId)).thenReturn(false);
        when(repo.count(postId)).thenReturn(5L);

        var result = service.toggle(postId, userId);

        assertTrue(result.liked());
        assertEquals(5L, result.count());
        verify(postService).getById(postId);
        verify(repo).save(postId, userId);
        verify(repo, never()).remove(postId, userId);
    }

    @Test
    @DisplayName("toggle removes the like when the post was already liked")
    void toggle_alreadyLiked_removesLike() {
        var postId = uuid(3);
        var userId = uuid(4);
        when(repo.exists(postId, userId)).thenReturn(true);
        when(repo.count(postId)).thenReturn(2L);

        var result = service.toggle(postId, userId);

        assertFalse(result.liked());
        assertEquals(2L, result.count());
        verify(repo).remove(postId, userId);
        verify(repo, never()).save(postId, userId);
    }

    @Test
    @DisplayName("toggle throws when the post does not exist")
    void toggle_missingPost_throws() {
        var postId = uuid(10);
        var userId = uuid(11);
        doThrow(new NotFoundException("Post not found")).when(postService).getById(postId);

        assertThrows(NotFoundException.class, () -> service.toggle(postId, userId));
        verify(repo, never()).save(postId, userId);
        verify(repo, never()).remove(postId, userId);
    }

    @Test
    @DisplayName("count and isLiked delegate to the repository")
    void countAndIsLikedDelegate() {
        var postId = uuid(5);
        var userId = uuid(6);
        when(repo.count(postId)).thenReturn(9L);
        when(repo.exists(postId, userId)).thenReturn(true);

        assertEquals(9L, service.count(postId));
        assertTrue(service.isLiked(postId, userId));
    }

    @Test
    @DisplayName("batch helpers delegate to the repository")
    void batchHelpersDelegate() {
        var ids = List.of(uuid(7), uuid(8));
        var userId = uuid(9);
        Map<UUID, Long> counts = Map.of(uuid(7), 3L);
        Set<UUID> liked = Set.of(uuid(8));
        when(repo.countByPostIds(ids)).thenReturn(counts);
        when(repo.likedPostIds(userId, ids)).thenReturn(liked);

        assertSame(counts, service.countByPostIds(ids));
        assertSame(liked, service.likedPostIds(userId, ids));
    }

    private static UUID uuid(int seed) {
        return new UUID(0, seed);
    }
}
