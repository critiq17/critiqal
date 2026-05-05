package org.critiqal.domain.post.repository;

import org.critiqal.domain.post.Post;

import java.util.List;
import java.util.Optional;

/**
 * Defines persistence operations for posts.
 * Supports feed queries, search, storage, and view counters.
 */
public interface PostRepository {

    List<Long> findLatestIds(int page, int size);

    long countPublished();

    List<Long> findByAuthorIds(Long authorId, int page, int size);

    long countByAuthor(Long authorId);

    List<Long> searchIds(String query, int page, int size);

    long countSearch(String query);

    List<Long> findFollowingFeedIds(Long userId, int page, int size);

    long countFollowingFeed(Long userId);

    List<Post> findByIdsWithRelations(List<Long> ids);

    Optional<Post> findByIdOptional(Long postId);

    Post save(Post post);

    void incrementViews(Long postId);
}
