package org.critiqal.domain.like.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.like.PostLike;
import org.critiqal.domain.shared.like.PostLikeId;

import java.util.UUID;

@ApplicationScoped
public class PostLikeRepositoryImpl
        implements PostLikeRepository, PanacheRepositoryBase<PostLike, PostLikeId> {

    @Override
    public boolean exists(UUID postId, UUID userId) {
        return findById(new PostLikeId(postId, userId)) != null;
    }

    @Override
    @Transactional
    public void save(UUID postId, UUID userId) {
        var like = new PostLike();
        like.id = new PostLikeId(postId, userId);
        persist(like);
    }

    @Override
    @Transactional
    public void remove(UUID postId, UUID userId) {
        var entity = findById(new PostLikeId(postId, userId));
        if (entity != null) {
            delete(entity);
        }
    }
    @Override
    public long count(UUID postId) {
        return count("id.postId", postId);
    }
}
