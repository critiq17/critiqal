package org.critiqal.domain.post;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.user.User;

import java.util.List;

/*
    PostRepository - impl methods for Post to postgres
 */

@ApplicationScoped
public class PostRepository implements PanacheRepository<Post> {

    public List<Long> findLatestIds(int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT p.id FROM Post p
                        WHERE p.status = :status
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, Long.class)
                .setParameter("status", PostStatus.PUBLISHED)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countPublished() {
        return count("status", PostStatus.PUBLISHED);
    }

    public List<Long> findByAuthorIds(Long authorId, int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT p.id FROM Post p
                        WHERE p.author.id = :authorId AND p.status = :status
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, Long.class)
                .setParameter("authorId", authorId)
                .setParameter("status", PostStatus.PUBLISHED)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countByAuthor(Long authorId) {
        return count("author.id = ?1 AND status = ?2", authorId, PostStatus.PUBLISHED);
    }

    /*
    public List<Post> search(String query, int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT DISTINCT p FROM Post p
                        LEFT JOIN FETCH p.author
                        LEFT JOIN FETCH p.photos
                        WHERE LOWER(p.content) LIKE :query AND p.status = :status
                        ORDER BY p.createdAt DESC
                        """, Post.class)
                .setParameter("query", "%" + query.toLowerCase() + "%")
                .setParameter("status", PostStatus.PUBLISHED)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
*/
    public List<Long> searchIds(String query, int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT p.id
                        FROM Post p
                        WHERE LOWER(p.content) LIKE :query
                          AND p.status = :status
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, Long.class)
                .setParameter("query", "%" + query.toLowerCase() + "%")
                .setParameter("status", PostStatus.PUBLISHED)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public List<Long> findFollowingFeedIds(Long userId, int page, int size) {
        return getEntityManager()
                .createQuery("""
                        SELECT p.id FROM Post p
                        WHERE p.status = :status
                          AND p.author.id IN (
                              SELECT f.following.id FROM Follow f
                              WHERE f.follower.id = :userId
                          )
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, Long.class)
                .setParameter("status", PostStatus.PUBLISHED)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countFollowingFeed(Long userId) {
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

    public List<Post> findByIdsWithRelations(List<Long> ids) {
        if (ids.isEmpty()) return List.of();

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
    public long countSearch(String query) {
        return count("LOWER(content) LIKE ?1 AND status = ?2",
                "%" + query.toLowerCase() + "%", PostStatus.PUBLISHED);
    }

    @Transactional
    public Post createPost(User author, String content) {
        var post = new Post();
        post.author = author;
        post.content = content;
        persist(post);
        return post;
    }

    @Transactional
    public void incrementViews(Long postId) {
        update("viewCount = viewCount + 1 WHERE id = ?1", postId);
    }
}