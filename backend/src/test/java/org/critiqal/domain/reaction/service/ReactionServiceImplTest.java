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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        var postId = uuid(6);
        var counts = Map.of(ReactionType.GIGACHAD, 3L);
        when(reactionRepo.countByPost(postId)).thenReturn(counts);

        assertSame(counts, service.getReactions(postId));
    }

    @Test
    void getMyReactionReturnsMappedTypeOrEmpty() {
        var postId = uuid(9);
        var firstUserId = uuid(4);
        var secondUserId = uuid(5);
        var reaction = new Reaction();
        reaction.type = ReactionType.DAVID;
        when(reactionRepo.findByPostAndUser(postId, firstUserId)).thenReturn(Optional.of(reaction));
        when(reactionRepo.findByPostAndUser(postId, secondUserId)).thenReturn(Optional.empty());

        assertEquals(Optional.of(ReactionType.DAVID), service.getMyReaction(postId, firstUserId));
        assertTrue(service.getMyReaction(postId, secondUserId).isEmpty());
    }

    @Test
    void reactUpdatesExistingReactionWithoutSavingNewEntity() {
        var userId = uuid(2);
        var postId = uuid(7);
        var reaction = new Reaction();
        reaction.type = ReactionType.GIGACHAD;
        when(userService.getById(userId)).thenReturn(user(userId));
        when(postService.getById(postId)).thenReturn(post(postId));
        when(reactionRepo.findByPostAndUser(postId, userId)).thenReturn(Optional.of(reaction));

        var updated = service.react(userId, postId, ReactionType.THE_ROCK);

        assertSame(reaction, updated);
        assertEquals(ReactionType.THE_ROCK, reaction.type);
        verify(reactionRepo, never()).save(any());
    }

    @Test
    void reactCreatesAndSavesNewReactionWhenMissing() {
        var userId = uuid(2);
        var postId = uuid(7);
        var user = user(userId);
        var post = post(postId);
        when(userService.getById(userId)).thenReturn(user);
        when(postService.getById(postId)).thenReturn(post);
        when(reactionRepo.findByPostAndUser(postId, userId)).thenReturn(Optional.empty());
        when(reactionRepo.save(any(Reaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.react(userId, postId, ReactionType.GIGACHAD);

        assertSame(user, created.user);
        assertSame(post, created.post);
        assertEquals(ReactionType.GIGACHAD, created.type);
        verify(reactionRepo).save(argThat(reaction ->
                reaction.user == user && reaction.post == post && reaction.type == ReactionType.GIGACHAD));
    }

    @Test
    void removeReactionDelegatesToRepository() {
        var userId = uuid(2);
        var postId = uuid(7);

        service.removeReaction(userId, postId);

        verify(reactionRepo).deleteByPostAndUser(postId, userId);
    }

    private static User user(UUID id) {
        var user = new User();
        user.id = id;
        return user;
    }

    private static Post post(UUID id) {
        var post = new Post();
        post.id = id;
        return post;
    }

    private static UUID uuid(int value) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(value));
    }
}
