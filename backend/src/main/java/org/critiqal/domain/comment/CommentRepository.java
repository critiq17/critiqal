package org.critiqal.domain.comment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.user.User;

import java.util.List;

// Comment repository
@ApplicationScoped
public class CommentRepository implements PanacheRepository<Comment> {

    // all comments by post
    public List<Comment> findByPost(Long postId) {
        return find("post.id = ?1 ORDER BY createdAt DESC", postId).list();
    }

    // add comment
    @Transactional
    public Comment addComment(User author, Post post, String content) {
        var comment = new Comment();
        comment.author = author;
        comment.post = post;
        comment.content = content;
        persist(comment);
        return comment;
    }

    // only root comments by post (without replies)
    public List<Comment> findByRootPost(Long postId) {
        return find("post.id = ?1 AND parent IS NULL ORDER BY createdAt ASC", postId).list();
    }

    // replies by comment
    public List<Comment> findReplies(Long parentId) {
        return find("parent.id = ?1 ORDER BY createdAt ASC", parentId).list();
    }

    // added reply to comment
    @Transactional
    public Comment addReply(User author, Post post, Comment parent, String content) {
        var reply = new Comment();
        reply.author = author;
        reply.post = post;
        reply.parent = parent;
        reply.content = content;
        persist(reply);
        return reply;
    }

    // delete comment
    @Transactional
    public void deleteComment(Long commentId, Long authorId) {
        delete("id = ?1 AND author.id = ?2", commentId, authorId);
    }
}
