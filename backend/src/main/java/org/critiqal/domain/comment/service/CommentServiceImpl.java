package org.critiqal.domain.comment.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import org.critiqal.domain.activity.ActivityEvent;
import org.critiqal.domain.comment.Comment;
import org.critiqal.domain.comment.repository.CommentRepository;
import org.critiqal.domain.post.repository.PostRepository;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link CommentService}.
 * Applies comment validation, reply rules, and authorization checks.
 */
@ApplicationScoped
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepo;
    private final PostService postService;
    private final UserService userService;
    private final PostRepository postRepo;
    private final Event<ActivityEvent> activityEvent;

    public CommentServiceImpl(CommentRepository commentRepo,
                              PostService postService,
                              UserService userService,
                              PostRepository postRepo,
                              Event<ActivityEvent> activityEvent) {
        this.commentRepo = commentRepo;
        this.postService = postService;
        this.userService = userService;
        this.postRepo = postRepo;
        this.activityEvent = activityEvent;
    }

    @Override
    public List<Comment> getPostComments(UUID postId) {
        postService.getById(postId);
        return commentRepo.findByPost(postId);
    }

    @Override
    public List<Comment> getRootComments(UUID postId) {
        postService.getById(postId);
        return commentRepo.findByRootPost(postId);
    }

    @Override
    public List<Comment> getReplies(UUID postId, UUID commentId) {
        var comment = findCommentInPost(postId, commentId);
        return commentRepo.findReplies(comment.id);
    }

    @Override
    @Transactional
    public Comment addComment(UUID authorId, UUID postId, String content) {
        if (content == null || content.isBlank()) {
            throw new DomainException("Comment cannot be empty");
        }

        var comment = new Comment();
        comment.author = userService.getById(authorId);
        comment.post = postService.getById(postId);
        comment.content = content;
        var saved = commentRepo.save(comment);

        postRepo.incrementCommentCount(postId, 1);
        activityEvent.fireAsync(new ActivityEvent(authorId, ActivityEvent.ActivityType.COMMENT_ADDED));
        return saved;
    }

    @Override
    @Transactional
    public Comment addReply(UUID authorId, UUID postId, UUID parentId, String content) {
        if (content == null || content.isBlank()) {
            throw new DomainException("Reply cannot be empty");
        }

        var post = postService.getById(postId);
        var parent = findCommentInPost(postId, parentId);
        if (parent.parent != null) {
            throw new ConflictException("Cannot reply to a reply");
        }

        var reply = new Comment();
        reply.author = userService.getById(authorId);
        reply.post = post;
        reply.parent = parent;
        reply.content = content;
        var saved = commentRepo.save(reply);

        postRepo.incrementCommentCount(postId, 1);
        activityEvent.fireAsync(new ActivityEvent(authorId, ActivityEvent.ActivityType.COMMENT_ADDED));
        return saved;
    }

    @Override
    @Transactional
    public void deleteComment(UUID postId, UUID commentId, UUID requestedId) {
        var comment = findCommentInPost(postId, commentId);
        if (!comment.author.id.equals(requestedId)) {
            throw new ForbiddenException("Not your comment");
        }

        int removed = 1;
        if (comment.parent == null) {
            removed += (int) commentRepo.countReplies(comment.id);
        }
        commentRepo.delete(comment);
        postRepo.decrementCommentCount(postId, removed);
    }

    private Comment findCommentInPost(UUID postId, UUID commentId) {
        postService.getById(postId);

        var comment = commentRepo.findByIdOptional(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        if (!comment.post.id.equals(postId)) {
            throw new NotFoundException("Comment not found");
        }
        return comment;
    }
}
