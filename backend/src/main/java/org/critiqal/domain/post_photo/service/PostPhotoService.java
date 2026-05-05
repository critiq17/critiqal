package org.critiqal.domain.post_photo.service;

import org.critiqal.domain.post_photo.PostPhoto;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;

/**
 * Defines post photo management operations.
 * Handles upload limits, ownership checks, and deletion flows.
 */
public interface PostPhotoService {

    PostPhoto addPhoto(Long postId, Long userId, FileUpload file) throws IOException;

    void deletePhoto(Long postId, Long userId, Long photoId);
}
