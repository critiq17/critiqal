package org.acme.domain.comment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.post.PostService;
import org.acme.domain.user.UserService;

import java.util.List;

@ApplicationScoped
public class CommentService {

    @Inject
    CommentRepository commentRepo;
    @Inject
    PostService postService;
    @Inject UserService userService;

    public List<Comment> getPostComments(Long postId) {
        postService.getById(postId);
        return commentRepo.findByPost(postId);
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
