package org.critiqal.domain.comment.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.comment.Comment;
import org.critiqal.domain.comment.repository.CommentRepository;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.service.UserService;

import java.util.List;

@ApplicationScoped
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepo;
    private final PostService postService;
    private final UserService userService;

    public CommentServiceImpl(CommentRepository commentRepo,
                              PostService postService,
                              UserService userService) {
        this.commentRepo = commentRepo;
        this.postService = postService;
        this.userService = userService;
    }

    @Override
    public List<Comment> getPostComments(Long postId) {
        postService.getById(postId);
        return commentRepo.findByPost(postId);
    }

    @Override
    public List<Comment> getRootComments(Long postId) {
        postService.getById(postId);
        return commentRepo.findByRootPost(postId);
    }

    @Override
    public List<Comment> getReplies(Long postId, Long commentId) {
        var comment = findCommentInPost(postId, commentId);
        return commentRepo.findReplies(comment.id);
    }

    @Override
    @Transactional
    public Comment addComment(Long authorId, Long postId, String content) {
        if (content == null || content.isBlank()) {
            throw new DomainException("Comment cannot be empty");
        }

        var comment = new Comment();
        comment.author = userService.getById(authorId);
        comment.post = postService.getById(postId);
        comment.content = content;
        return commentRepo.save(comment);
    }

    @Override
    @Transactional
    public Comment addReply(Long authorId, Long postId, Long parentId, String content) {
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
        return commentRepo.save(reply);
    }

    @Override
    @Transactional
    public void deleteComment(Long postId, Long commentId, Long requestedId) {
        var comment = findCommentInPost(postId, commentId);
        if (!comment.author.id.equals(requestedId)) {
            throw new ForbiddenException("Not your comment");
        }
        commentRepo.delete(comment);
    }

    private Comment findCommentInPost(Long postId, Long commentId) {
        postService.getById(postId);

        var comment = commentRepo.findByIdOptional(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        if (!comment.post.id.equals(postId)) {
            throw new NotFoundException("Comment not found");
        }
        return comment;
    }
}
