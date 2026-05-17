package org.critiqal.domain.like.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.like.CommentLike;
import org.critiqal.domain.shared.like.CommentLikeId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CommentLikeRepositoryImpl
        implements CommentLikeRepository, PanacheRepositoryBase<CommentLike, CommentLikeId> {

    @Override
    public boolean exists(UUID commentId, UUID userId) {
        return findById(new CommentLikeId(commentId, userId)) != null;
    }

    @Override
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

    @Override
    public Map<UUID, Long> countByCommentIds(List<UUID> commentIds) {
        if (commentIds.isEmpty()) return Map.of();

        List<Object[]> rows = getEntityManager()
                .createQuery("""
                        SELECT cl.id.commentId, COUNT(cl)
                        FROM CommentLike cl
                        WHERE cl.id.commentId IN :ids
                        GROUP BY cl.id.commentId
                        """, Object[].class)
                .setParameter("ids", commentIds)
                .getResultList();

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    public Set<UUID> likedCommentIds(UUID userId, List<UUID> commentIds) {
        if (commentIds.isEmpty()) return Set.of();

        return getEntityManager()
                .createQuery("""
                        SELECT cl.id.commentId
                        FROM CommentLike cl
                        WHERE cl.id.userId = :userId
                          AND cl.id.commentId IN :ids
                        """, UUID.class)
                .setParameter("userId", userId)
                .setParameter("ids", commentIds)
                .getResultList()
                .stream()
                .collect(Collectors.toSet());
    }
}
