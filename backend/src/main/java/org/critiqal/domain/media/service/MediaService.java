package org.critiqal.domain.media.service;

import org.critiqal.domain.post.Post;

import java.io.IOException;
import java.io.InputStream;

/**
 * Defines media management operations for avatars and post photos.
 * Handles uploads and cleanup of stored assets.
 */
public interface MediaService {
    String uploadAvatar(Long userId, InputStream imageStream) throws IOException;
    String uploadPostPhoto(Post post, InputStream imageStream, String contentType) throws IOException;
    void deleteAllPostPhotos(Long postId);
    void deletePhoto(String url);
}
