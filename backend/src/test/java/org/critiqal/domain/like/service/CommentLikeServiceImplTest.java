package org.critiqal.domain.like.service;

import org.critiqal.domain.comment.Comment;
import org.critiqal.domain.comment.repository.CommentRepository;
import org.critiqal.domain.like.repository.CommentLikeRepository;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentLikeServiceImplTest {

    private final CommentLikeRepository repo = mock(CommentLikeRepository.class);
    private final CommentRepository commentRepo = mock(CommentRepository.class);

    private CommentLikeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommentLikeServiceImpl(repo, commentRepo);
    }

    @Test
    @DisplayName("toggle adds a like when the comment was not yet liked")
    void toggle_notLiked_addsLike() {
        var commentId = uuid(1);
        var userId = uuid(2);
        when(commentRepo.findByIdOptional(commentId)).thenReturn(Optional.of(new Comment()));
        when(repo.exists(commentId, userId)).thenReturn(false);
        when(repo.count(commentId)).thenReturn(4L);

        var result = service.toggle(commentId, userId);

        assertTrue(result.liked());
        assertEquals(4L, result.count());
        verify(repo).save(commentId, userId);
        verify(repo, never()).remove(commentId, userId);
    }

    @Test
    @DisplayName("toggle removes the like when the comment was already liked")
    void toggle_alreadyLiked_removesLike() {
        var commentId = uuid(3);
        var userId = uuid(4);
        when(commentRepo.findByIdOptional(commentId)).thenReturn(Optional.of(new Comment()));
        when(repo.exists(commentId, userId)).thenReturn(true);
        when(repo.count(commentId)).thenReturn(1L);

        var result = service.toggle(commentId, userId);

        assertFalse(result.liked());
        assertEquals(1L, result.count());
        verify(repo).remove(commentId, userId);
        verify(repo, never()).save(commentId, userId);
    }

    @Test
    @DisplayName("toggle throws when the comment does not exist")
    void toggle_missingComment_throws() {
        var commentId = uuid(5);
        when(commentRepo.findByIdOptional(commentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.toggle(commentId, uuid(6)));
        verify(repo, never()).save(commentId, uuid(6));
    }

    @Test
    @DisplayName("batch helpers delegate to the repository")
    void batchHelpersDelegate() {
        var ids = List.of(uuid(7), uuid(8));
        var userId = uuid(9);
        Map<UUID, Long> counts = Map.of(uuid(7), 2L);
        Set<UUID> liked = Set.of(uuid(8));
        when(repo.countByCommentIds(ids)).thenReturn(counts);
        when(repo.likedCommentIds(userId, ids)).thenReturn(liked);

        assertSame(counts, service.countByCommentIds(ids));
        assertSame(liked, service.likedCommentIds(userId, ids));
    }

    private static UUID uuid(int seed) {
        return new UUID(0, seed);
    }
}
