package org.critiqal.domain.post.repository;

import org.critiqal.domain.post.Post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines persistence operations for posts.
 * Supports feed queries, search, storage, and view counters.
 */
public interface PostRepository {

    List<UUID> findLatestIds(int page, int size);

    long countPublished();

    List<UUID> findByAuthorIds(UUID authorId, int page, int size);

    long countByAuthor(UUID authorId);

    List<UUID> searchIds(String query, int page, int size);

    long countSearch(String query);

    List<UUID> findFollowingFeedIds(UUID userId, int page, int size);

    long countFollowingFeed(UUID userId);

    List<Post> findByIdsWithRelations(List<UUID> ids);

    Optional<Post> findByIdOptional(UUID postId);

    Post save(Post post);

    void incrementViews(UUID postId);
}
