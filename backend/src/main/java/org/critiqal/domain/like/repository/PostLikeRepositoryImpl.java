package org.critiqal.domain.like.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.SecondaryTable;
import jakarta.transaction.Transactional;
import org.critiqal.domain.like.PostLike;
import org.critiqal.domain.shared.like.PostLikeId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

    @Override
    public Map<UUID, Long> countByPostIds(List<UUID> postIds) {
        if (postIds.isEmpty()) return Map.of();

        List<Object[]> rows = getEntityManager()
                .createQuery("""
                        SELECT pl.id.postId, COUNT(pl)
                        FROM PostLike pl
                        WHERE pl.id.postId IN :ids
                        GROUP BY pl.id.postId 
                        """, Object[].class)
                .setParameter("ids", postIds)
                .getResultList();

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    public Set<UUID> likedPostIds(UUID userId, List<UUID> postIds) {
        if (postIds.isEmpty()) return Set.of();

        return getEntityManager()
                .createQuery("""
                        SELECT pl.id.postId
                        FROM PostLike pl
                        WHERE pl.id.userId = :userId
                          AND pl.id.postId IN :ids
                        """, UUID.class)
                .setParameter("userId", userId)
                .setParameter("ids", postIds)
                .getResultList()
                .stream()
                .collect(Collectors.toSet());
    }
}
