package org.critiqal.domain.like.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.like.CommentLike;
import org.critiqal.domain.shared.like.CommentLikeId;

import java.util.UUID;

@ApplicationScoped
public class CommentLikeRepositoryImpl implements CommentLikeRepository, PanacheRepositoryBase<CommentLike, CommentLikeId> {

    public boolean exists(UUID commentId, UUID userId) {
        return findById(new CommentLikeId(commentId, userId)) != null;
    }

    @Transactional
    public void save(UUID commentId, UUID userId) {
        var like = new CommentLike();
        like.id = new CommentLikeId(commentId, userId);
        persist(like);
    }

    @Override
    @Transactional
    public void remove(UUID commentId, UUID userId) {
        var entity = findById(new CommentLikeId(commentId, userId));
        if (entity != null) {
            delete(entity);
        }
    }
    @Override
    public long count(UUID commentId) {
        return count("id.commentId", commentId);
    }
}
