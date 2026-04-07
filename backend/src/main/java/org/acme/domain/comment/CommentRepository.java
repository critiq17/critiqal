package org.acme.domain.comment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.domain.post.Post;
import org.acme.domain.user.User;

import java.util.List;

@ApplicationScoped
public class CommentRepository implements PanacheRepository<Comment> {

    public List<Comment> findByPost(Long postId) {
        return find("post.id = ?1 ORDER BY createdAt DESC", postId).list();
    }

    @Transactional
    public Comment addComment(User author, Post post, String content) {
        var comment = new Comment();
        comment.author = author;
        comment.post = post;
        comment.content = content;
        persist(comment);
        return comment;
    }

    @Transactional
    public void deleteComment(Long commentId, Long authorId) {
        delete("id = ?1 AND author.id = ?2", commentId, authorId);
    }
}
