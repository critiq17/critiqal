package org.critiqal.domain.post.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.post.PostStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Panache-backed implementation of {@link PostRepository}.
 * Executes feed, search, and relation-aware post queries.
 */
@ApplicationScoped
public class PostRepositoryImpl implements PostRepository, PanacheRepository<Post> {

    @Override
    public List<UUID> findLatestIds(int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT p.id FROM Post p
                        WHERE p.status = :status
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, UUID.class)
                .setParameter("status", PostStatus.PUBLISHED)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public long countPublished() {
        return count("status", PostStatus.PUBLISHED);
    }

    @Override
    public List<UUID> findByAuthorIds(UUID authorId, int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT p.id FROM Post p
                        WHERE p.author.id = :authorId AND p.status = :status
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, UUID.class)
                .setParameter("authorId", authorId)
                .setParameter("status", PostStatus.PUBLISHED)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public long countByAuthor(UUID authorId) {
        return count("author.id = ?1 AND status = ?2", authorId, PostStatus.PUBLISHED);
    }

    @Override
    public List<UUID> searchIds(String query, int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT p.id
                        FROM Post p
                        WHERE LOWER(p.content) LIKE :query
                          AND p.status = :status
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, UUID.class)
                .setParameter("query", "%" + query.toLowerCase() + "%")
                .setParameter("status", PostStatus.PUBLISHED)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public long countSearch(String query) {
        return count("LOWER(content) LIKE ?1 AND status = ?2",
                "%" + query.toLowerCase() + "%", PostStatus.PUBLISHED);
    }

    @Override
    public List<UUID> findFollowingFeedIds(UUID userId, int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT p.id FROM Post p
                        WHERE p.status = :status
                          AND p.author.id IN (
                              SELECT f.following.id FROM Follow f
                              WHERE f.follower.id = :userId
                          )
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, UUID.class)
                .setParameter("status", PostStatus.PUBLISHED)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public long countFollowingFeed(UUID userId) {
        return getEntityManager()
                .createQuery("""
                        SELECT COUNT(p) FROM Post p
                        WHERE p.status = :status
                          AND p.author.id IN (
                              SELECT f.following.id FROM Follow f
                              WHERE f.follower.id = :userId
                          )
                        """, Long.class)
                .setParameter("status", PostStatus.PUBLISHED)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Override
    public List<Post> findByIdsWithRelations(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return getEntityManager()
                .createQuery("""
                        SELECT DISTINCT p
                        FROM Post p
                        LEFT JOIN FETCH p.author
                        LEFT JOIN FETCH p.photos
                        WHERE p.id IN :ids
                        """, Post.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public Optional<Post> findByIdOptional(UUID postId) {
        return find("id", postId).firstResultOptional();
    }

    @Override
    @Transactional
    public Post save(Post post) {
        persist(post);
        return post;
    }

    @Override
    @Transactional
    public void incrementViews(UUID postId) {
        update("viewCount = viewCount + 1 WHERE id = ?1", postId);
    }

    @Override
    @Transactional
    public void incrementLikeCount(UUID postId) {
        update("likeCount = likeCount + 1 WHERE id = ?1", postId);
    }

    @Override
    @Transactional
    public void decrementLikeCount(UUID postId) {
        getEntityManager()
                .createNativeQuery(
                        "UPDATE posts SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?1"
                ).setParameter(1, postId)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void incrementCommentCount(UUID postId, int delta) {
        update("commentCount = commentCount + ?1 WHERE id = ?2", delta, postId);
    }

    @Override
    @Transactional
    public void decrementCommentCount(UUID postId, int delta){
        getEntityManager()
                .createNativeQuery(
                        "UPDATE posts SET comment_count = GREATEST(comment_count - ?1, 0) WHERE id = ?2"
                ).setParameter(1, delta)
                .setParameter(2, postId)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void recount(UUID postId) {
        getEntityManager().createNativeQuery("""
                UPDATE posts p SET
                    like_count = COALESCE(
                        (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id), 0)
                    comment_count = COALESCE(
                        (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id), 0)
                WHERE p.id = ?1
                """)
                .setParameter(1, postId)
                .executeUpdate();
    }
}
