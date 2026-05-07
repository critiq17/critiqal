package org.critiqal.domain.post_photo.repository;

import org.critiqal.domain.post_photo.PostPhoto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines persistence operations for post photos.
 * Supports per-post lookup, storage, and deletion.
 */
public interface PostPhotoRepository {

    List<PostPhoto> findByPost(UUID postId);

    long countByPost(UUID postId);

    Optional<PostPhoto> findByIdOptional(UUID photoId);

    PostPhoto save(PostPhoto photo);

    void delete(PostPhoto photo);

    void deleteByPost(UUID postId);
}
