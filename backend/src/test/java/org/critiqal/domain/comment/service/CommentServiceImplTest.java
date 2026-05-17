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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        var post = post(8, 1);
        var comments = List.of(comment(1, post, 2));
        when(postService.getById(uuid(8))).thenReturn(post);
        when(commentRepo.findByPost(uuid(8))).thenReturn(comments);

        assertSame(comments, service.getPostComments(uuid(8)));
        verify(postService).getById(uuid(8));
        verify(commentRepo).findByPost(uuid(8));
    }

    @Test
    void getRootCommentsValidatesPostAndReturnsRepositoryResult() {
        var post = post(8, 1);
        var comments = List.of(comment(2, post, 2));
        when(postService.getById(uuid(8))).thenReturn(post);
        when(commentRepo.findByRootPost(uuid(8))).thenReturn(comments);

        assertSame(comments, service.getRootComments(uuid(8)));
        verify(commentRepo).findByRootPost(uuid(8));
    }

    @Test
    void getRepliesThrowsWhenCommentBelongsToAnotherPost() {
        var comment = comment(3, post(99, 1), 2);
        when(postService.getById(uuid(8))).thenReturn(post(8, 1));
        when(commentRepo.findByIdOptional(uuid(3))).thenReturn(Optional.of(comment));

        assertThrows(NotFoundException.class, () -> service.getReplies(uuid(8), uuid(3)));
    }

    @Test
    void addCommentRejectsBlankContent() {
        assertThrows(DomainException.class, () -> service.addComment(uuid(1), uuid(2), " "));
    }

    @Test
    void addCommentBuildsAndPersistsComment() {
        var author = user(4);
        var post = post(6, 10);
        when(userService.getById(uuid(4))).thenReturn(author);
        when(postService.getById(uuid(6))).thenReturn(post);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.addComment(uuid(4), uuid(6), "Nice ride");

        assertSame(author, saved.author);
        assertSame(post, saved.post);
        assertEquals("Nice ride", saved.content);
    }

    @Test
    void addReplyRejectsBlankContent() {
        assertThrows(DomainException.class, () -> service.addReply(uuid(1), uuid(2), uuid(3), ""));
    }

    @Test
    void addReplyRejectsReplyToReply() {
        var post = post(6, 10);
        var parent = comment(7, post, 2);
        parent.parent = comment(8, post, 3);
        when(postService.getById(uuid(6))).thenReturn(post);
        when(commentRepo.findByIdOptional(uuid(7))).thenReturn(Optional.of(parent));

        assertThrows(ConflictException.class, () -> service.addReply(uuid(4), uuid(6), uuid(7), "Nested"));
        verify(userService, never()).getById(any());
        verify(commentRepo, never()).save(any());
    }

    @Test
    void addReplyBuildsAndPersistsReply() {
        var author = user(4);
        var post = post(6, 10);
        var parent = comment(7, post, 2);
        when(postService.getById(uuid(6))).thenReturn(post);
        when(commentRepo.findByIdOptional(uuid(7))).thenReturn(Optional.of(parent));
        when(userService.getById(uuid(4))).thenReturn(author);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.addReply(uuid(4), uuid(6), uuid(7), "Reply");

        assertSame(author, saved.author);
        assertSame(post, saved.post);
        assertSame(parent, saved.parent);
        assertEquals("Reply", saved.content);
    }

    @Test
    void deleteCommentThrowsWhenRequesterIsNotOwner() {
        var target = comment(11, post(6, 10), 2);
        when(postService.getById(uuid(6))).thenReturn(post(6, 10));
        when(commentRepo.findByIdOptional(uuid(11))).thenReturn(Optional.of(target));

        assertThrows(ForbiddenException.class, () -> service.deleteComment(uuid(6), uuid(11), uuid(99)));
        verify(commentRepo, never()).delete(any());
    }

    @Test
    void deleteCommentDeletesOwnedComment() {
        var target = comment(11, post(6, 10), 2);
        when(postService.getById(uuid(6))).thenReturn(post(6, 10));
        when(commentRepo.findByIdOptional(uuid(11))).thenReturn(Optional.of(target));

        service.deleteComment(uuid(6), uuid(11), uuid(2));

        verify(commentRepo).delete(target);
    }

    private static Comment comment(long id, Post post, long authorId) {
        var comment = new Comment();
        comment.id = uuid(id);
        comment.post = post;
        comment.author = user(authorId);
        return comment;
    }

    private static Post post(long postId, long authorId) {
        var post = new Post();
        post.id = uuid(postId);
        post.author = user(authorId);
        return post;
    }

    private static User user(long id) {
        var user = new User();
        user.id = uuid(id);
        return user;
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
