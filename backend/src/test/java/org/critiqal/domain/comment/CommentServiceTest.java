package org.critiqal.domain.comment;

import org.critiqal.domain.post.Post;
import org.critiqal.domain.post.PostService;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    private final CommentRepository commentRepo = mock(CommentRepository.class);
    private final PostService  postService = mock(PostService.class);
    private UserService userService = mock(UserService.class);

    private final CommentService commentService = new CommentService(commentRepo, postService, userService);

    @Test
    void addComment_blankContent_throws() {
        assertThrows(DomainException.class,
                () -> commentService.addComment(1L, 1L, ""));
    }

    @Test
    void addReply_toReply_throws() {
        var parent = new Comment();
        parent.parent = new Comment();

        when(commentRepo.findByIdOptional(5L)).thenReturn(Optional.of(parent));
        when(userService.getById(any())).thenReturn(new User());
        when(postService.getById(any())).thenReturn(new Post());

        assertThrows(ConflictException.class,
                () -> commentService.addReply(1L, 1L, 5L, "reply to reply"));
    }

    @Test
    void deleteComment_notOwner_throws() {
        var author = new User();
        author.id = 2L;
        var comment = new Comment();
        comment.author = author;

        when(commentRepo.findByIdOptional(1L)).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenException.class,
                () -> commentService.deleteComment(1L, 99L));
    }
}
