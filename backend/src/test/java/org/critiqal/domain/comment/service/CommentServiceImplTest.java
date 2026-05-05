package org.critiqal.domain.comment.service;

import org.critiqal.domain.comment.Comment;
import org.critiqal.domain.comment.repository.CommentRepository;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceImplTest {

    private final CommentRepository commentRepo = mock(CommentRepository.class);
    private final PostService postService = mock(PostService.class);
    private final UserService userService = mock(UserService.class);

    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommentServiceImpl(commentRepo, postService, userService);
    }

    @Test
    void getPostCommentsValidatesPostAndReturnsRepositoryResult() {
        var post = post(8L, 1L);
        var comments = List.of(comment(1L, post, 2L));
        when(postService.getById(8L)).thenReturn(post);
        when(commentRepo.findByPost(8L)).thenReturn(comments);

        assertSame(comments, service.getPostComments(8L));
        verify(postService).getById(8L);
        verify(commentRepo).findByPost(8L);
    }

    @Test
    void getRootCommentsValidatesPostAndReturnsRepositoryResult() {
        var post = post(8L, 1L);
        var comments = List.of(comment(2L, post, 2L));
        when(postService.getById(8L)).thenReturn(post);
        when(commentRepo.findByRootPost(8L)).thenReturn(comments);

        assertSame(comments, service.getRootComments(8L));
        verify(commentRepo).findByRootPost(8L);
    }

    @Test
    void getRepliesThrowsWhenCommentBelongsToAnotherPost() {
        var comment = comment(3L, post(99L, 1L), 2L);
        when(postService.getById(8L)).thenReturn(post(8L, 1L));
        when(commentRepo.findByIdOptional(3L)).thenReturn(Optional.of(comment));

        assertThrows(NotFoundException.class, () -> service.getReplies(8L, 3L));
    }

    @Test
    void addCommentRejectsBlankContent() {
        assertThrows(DomainException.class, () -> service.addComment(1L, 2L, " "));
    }

    @Test
    void addCommentBuildsAndPersistsComment() {
        var author = user(4L);
        var post = post(6L, 10L);
        when(userService.getById(4L)).thenReturn(author);
        when(postService.getById(6L)).thenReturn(post);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.addComment(4L, 6L, "Nice ride");

        assertSame(author, saved.author);
        assertSame(post, saved.post);
        assertEquals("Nice ride", saved.content);
    }

    @Test
    void addReplyRejectsBlankContent() {
        assertThrows(DomainException.class, () -> service.addReply(1L, 2L, 3L, ""));
    }

    @Test
    void addReplyRejectsReplyToReply() {
        var post = post(6L, 10L);
        var parent = comment(7L, post, 2L);
        parent.parent = comment(8L, post, 3L);
        when(postService.getById(6L)).thenReturn(post);
        when(commentRepo.findByIdOptional(7L)).thenReturn(Optional.of(parent));

        assertThrows(ConflictException.class, () -> service.addReply(4L, 6L, 7L, "Nested"));
        verify(userService, never()).getById(any());
        verify(commentRepo, never()).save(any());
    }

    @Test
    void addReplyBuildsAndPersistsReply() {
        var author = user(4L);
        var post = post(6L, 10L);
        var parent = comment(7L, post, 2L);
        when(postService.getById(6L)).thenReturn(post);
        when(commentRepo.findByIdOptional(7L)).thenReturn(Optional.of(parent));
        when(userService.getById(4L)).thenReturn(author);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.addReply(4L, 6L, 7L, "Reply");

        assertSame(author, saved.author);
        assertSame(post, saved.post);
        assertSame(parent, saved.parent);
        assertEquals("Reply", saved.content);
    }

    @Test
    void deleteCommentThrowsWhenRequesterIsNotOwner() {
        var target = comment(11L, post(6L, 10L), 2L);
        when(postService.getById(6L)).thenReturn(post(6L, 10L));
        when(commentRepo.findByIdOptional(11L)).thenReturn(Optional.of(target));

        assertThrows(ForbiddenException.class, () -> service.deleteComment(6L, 11L, 99L));
        verify(commentRepo, never()).delete(any());
    }

    @Test
    void deleteCommentDeletesOwnedComment() {
        var target = comment(11L, post(6L, 10L), 2L);
        when(postService.getById(6L)).thenReturn(post(6L, 10L));
        when(commentRepo.findByIdOptional(11L)).thenReturn(Optional.of(target));

        service.deleteComment(6L, 11L, 2L);

        verify(commentRepo).delete(target);
    }

    private static Comment comment(Long id, Post post, Long authorId) {
        var comment = new Comment();
        comment.id = id;
        comment.post = post;
        comment.author = user(authorId);
        return comment;
    }

    private static Post post(Long postId, Long authorId) {
        var post = new Post();
        post.id = postId;
        post.author = user(authorId);
        return post;
    }

    private static User user(Long id) {
        var user = new User();
        user.id = id;
        return user;
    }
}
