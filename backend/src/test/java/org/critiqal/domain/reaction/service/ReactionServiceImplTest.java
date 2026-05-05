package org.critiqal.domain.reaction.service;

import org.critiqal.domain.post.Post;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.reaction.Reaction;
import org.critiqal.domain.reaction.ReactionType;
import org.critiqal.domain.reaction.repository.ReactionRepository;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactionServiceImplTest {

    private final ReactionRepository reactionRepo = mock(ReactionRepository.class);
    private final PostService postService = mock(PostService.class);
    private final UserService userService = mock(UserService.class);

    private ReactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReactionServiceImpl(reactionRepo, postService, userService);
    }

    @Test
    void getReactionsDelegatesToRepository() {
        var counts = Map.of(ReactionType.GIGACHAD, 3L);
        when(reactionRepo.countByPost(6L)).thenReturn(counts);

        assertSame(counts, service.getReactions(6L));
    }

    @Test
    void getMyReactionReturnsMappedTypeOrEmpty() {
        var reaction = new Reaction();
        reaction.type = ReactionType.DAVID;
        when(reactionRepo.findByPostAndUser(9L, 4L)).thenReturn(Optional.of(reaction));
        when(reactionRepo.findByPostAndUser(9L, 5L)).thenReturn(Optional.empty());

        assertEquals(Optional.of(ReactionType.DAVID), service.getMyReaction(9L, 4L));
        assertTrue(service.getMyReaction(9L, 5L).isEmpty());
    }

    @Test
    void reactUpdatesExistingReactionWithoutSavingNewEntity() {
        var reaction = new Reaction();
        reaction.type = ReactionType.GIGACHAD;
        when(userService.getById(2L)).thenReturn(user(2L));
        when(postService.getById(7L)).thenReturn(post(7L));
        when(reactionRepo.findByPostAndUser(7L, 2L)).thenReturn(Optional.of(reaction));

        var updated = service.react(2L, 7L, ReactionType.THE_ROCK);

        assertSame(reaction, updated);
        assertEquals(ReactionType.THE_ROCK, reaction.type);
        verify(reactionRepo, never()).save(any());
    }

    @Test
    void reactCreatesAndSavesNewReactionWhenMissing() {
        var user = user(2L);
        var post = post(7L);
        when(userService.getById(2L)).thenReturn(user);
        when(postService.getById(7L)).thenReturn(post);
        when(reactionRepo.findByPostAndUser(7L, 2L)).thenReturn(Optional.empty());
        when(reactionRepo.save(any(Reaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.react(2L, 7L, ReactionType.GIGACHAD);

        assertSame(user, created.user);
        assertSame(post, created.post);
        assertEquals(ReactionType.GIGACHAD, created.type);
        verify(reactionRepo).save(argThat(reaction ->
                reaction.user == user && reaction.post == post && reaction.type == ReactionType.GIGACHAD));
    }

    @Test
    void removeReactionDelegatesToRepository() {
        service.removeReaction(2L, 7L);

        verify(reactionRepo).deleteByPostAndUser(7L, 2L);
    }

    private static User user(Long id) {
        var user = new User();
        user.id = id;
        return user;
    }

    private static Post post(Long id) {
        var post = new Post();
        post.id = id;
        return post;
    }
}
