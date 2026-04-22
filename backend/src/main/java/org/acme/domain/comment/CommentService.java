package org.acme.domain.comment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.post.PostService;
import org.acme.domain.user.UserService;

import java.util.List;

@ApplicationScoped
public class CommentService {

    private final CommentRepository commentRepo;
    private final PostService postService;
    private final UserService userService;

    public CommentService(CommentRepository commentRepo,
                          PostService postService,
                          UserService userService) {
        this.commentRepo = commentRepo;
        this.postService = postService;
        this.userService = userService;
    }

    // all post comments
    public List<Comment> getPostComments(Long postId) {
        postService.getById(postId);
        return commentRepo.findByPost(postId);
    }

    // only root post comments
    public List<Comment> getRootComments(Long postId) {
        postService.getById(postId);
        return commentRepo.findByRootPost(postId);
    }

    // replies by comment
    public List<Comment> getReplies(Long commentId) {
        return commentRepo.findReplies(commentId);
    }

    @Transactional
    public Comment addComment(Long authorId, Long postId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }
        var author = userService.getById(authorId);
        var post = postService.getById(postId);
        return commentRepo.addComment(author, post, content);
    }

    // add reply to comment
    @Transactional
    public Comment addReply(Long authorId, Long postId, Long parentId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Reply cannot be empty");
        }

        var author = userService.getById(authorId);
        var post = postService.getById(postId);
        var parent = commentRepo.findByIdOptional(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (parent.parent != null) {
            throw new IllegalArgumentException("Cannot reply to a reply");
        }

        return commentRepo.addReply(author, post, parent, content);
    }

    // delete comment
    @Transactional
    public void deleteComment(Long commentId, Long requestedId) {
        var comment = commentRepo.findByIdOptional(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        if (!comment.author.id.equals(requestedId)) {
            throw new IllegalArgumentException("Not your comment");
        }
        commentRepo.deleteComment(commentId, requestedId);
    }
}
