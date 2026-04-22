package org.critiqal.domain.post;

import jakarta.enterprise.event.Event;
import org.acme.domain.post.*;
import org.acme.domain.user.User;
import org.acme.domain.user.UserService;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/*
    PostServiceTest - unit tests for Post
    Testing permissions by delete, post, get
 */

public class PostServiceTest {

    private final PostRepository postRepo = mock(PostRepository.class);
    private final UserService userService = mock(UserService.class);
    private final Event<PostCreatedEvent> event = mock(Event.class);

    private final PostService postService = new PostService(postRepo, userService, event);

    @Test
    void createPost_blankContent_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> postService.createPost(1L, "  "));
    }

    @Test
    void createPost_nullContent_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> postService.createPost(1L, null));
    }

    @Test
    void deletePost_notOwner_throwsException() {
        var author = new User();
        author.id = 2L;
        var post = new Post();
        post.author = author;

        when(postRepo.findByIdOptional(1L)).thenReturn(Optional.of(post));

        assertThrows(IllegalArgumentException.class,
                () -> postService.deletePost(1L, 99L));
    }

    @Test
    void deletePost_owner_setsStatusDeleted() {
        var author = new User();
        author.id = 1L;
        var post = new Post();
        post.author = author;
        post.status = PostStatus.PUBLISHED;

        when(postRepo.findByIdOptional(1L)).thenReturn(Optional.of(post));
        postService.deletePost(1L, 1L);
        assertEquals(PostStatus.DELETED, post.status);
    }

    @Test
    void getById_notFound_throwsException() {
        when(postRepo.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> postService.getById(99L));
    }

    @Test
    void search_blankQuery_returnsEmpty() {
        var result = postService.search("  ", 0, 20);
        assertTrue(result.content().isEmpty());
        verifyNoInteractions(postRepo);
    }
}

