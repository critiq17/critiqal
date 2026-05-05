package org.critiqal.domain.comment.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.comment.Comment;

import java.util.List;
import java.util.Optional;

/**
 * Panache-backed implementation of {@link CommentRepository}.
 * Persists comments and reply lookups for posts.
 */
@ApplicationScoped
public class CommentRepositoryImpl implements CommentRepository, PanacheRepository<Comment> {

    @Override
    public List<Comment> findByPost(Long postId) {
        return find("post.id = ?1 ORDER BY createdAt DESC", postId).list();
    }

    @Override
    public List<Comment> findByRootPost(Long postId) {
        return find("post.id = ?1 AND parent IS NULL ORDER BY createdAt ASC", postId).list();
    }

    @Override
    public List<Comment> findReplies(Long parentId) {
        return find("parent.id = ?1 ORDER BY createdAt ASC", parentId).list();
    }

    @Override
    public Optional<Comment> findByIdOptional(Long commentId) {
        return find("id", commentId).firstResultOptional();
    }

    @Override
    @Transactional
    public Comment save(Comment comment) {
        persist(comment);
        return comment;
    }

    @Override
    @Transactional
    public void delete(Comment comment) {
        delete("id", comment.id);
    }
}
