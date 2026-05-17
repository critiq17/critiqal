package org.critiqal.domain.post_photo.service;

import org.critiqal.domain.post_photo.PostPhoto;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.util.UUID;

/**
 * Defines post photo management operations.
 * Handles upload limits, ownership checks, and deletion flows.
 */
public interface PostPhotoService {

    PostPhoto addPhoto(UUID postId, UUID userId, FileUpload file) throws IOException;

    void deletePhoto(UUID postId, UUID userId, UUID photoId);
}
