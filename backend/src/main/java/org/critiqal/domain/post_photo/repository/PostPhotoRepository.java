package org.critiqal.domain.post_photo.repository;

import org.critiqal.domain.post_photo.PostPhoto;

import java.util.List;
import java.util.Optional;

/**
 * Defines persistence operations for post photos.
 * Supports per-post lookup, storage, and deletion.
 */
public interface PostPhotoRepository {

    List<PostPhoto> findByPost(Long postId);

    long countByPost(Long postId);

    Optional<PostPhoto> findByIdOptional(Long photoId);

    PostPhoto save(PostPhoto photo);

    void delete(PostPhoto photo);

    void deleteByPost(Long postId);
}
