package org.critiqal.domain.post.service;

import org.critiqal.domain.post.Post;
import org.critiqal.domain.shared.pagination.Page;

/**
 * Defines post management operations.
 * Handles creation, retrieval, feed queries, search, and view tracking.
 */
public interface PostService {

    Post createPost(Long authorId, String content);

    Page<Post> getUserPost(Long authorId, int page, int size);

    Page<Post> getLatestFeed(int page, int size);

    Post getById(Long postId);

    Page<Post> search(String query, int page, int size);

    Page<Post> getFollowingFeed(Long userId, int page, int size);

    void view(Long postId, Long userId);

    void deletePost(Long postId, Long requestedId);

    void view(Long postId);
}
