package org.critiqal.domain.post.service;

import org.critiqal.domain.post.Post;
import org.critiqal.domain.shared.pagination.Page;

import java.util.UUID;

/**
 * Defines post management operations.
 * Handles creation, retrieval, feed queries, search, and view tracking.
 */
public interface PostService {

    Post createPost(UUID authorId, String content);

    Page<Post> getUserPost(UUID authorId, int page, int size);

    long countByAuthor(UUID authorId);

    Page<Post> getLatestFeed(int page, int size);

    Post getById(UUID postId);

    Page<Post> search(String query, int page, int size);

    Page<Post> getFollowingFeed(UUID userId, int page, int size);

    void view(UUID postId, UUID userId);

    void deletePost(UUID postId, UUID requestedId);

    void view(UUID postId);
}
