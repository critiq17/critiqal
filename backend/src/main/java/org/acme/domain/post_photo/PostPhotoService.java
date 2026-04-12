package org.acme.domain.post_photo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.acme.domain.post.PostService;
import org.acme.infra.storage.services.MediaService;
import org.acme.utils.ImageProcessor;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

@ApplicationScoped
public class PostPhotoService {

    @Inject PostPhotoRepository postPhotoRepo;
    @Inject PostService postService;
    @Inject MediaService mediaService;
    @Inject ImageProcessor imageProcessor;

    @Transactional
    public PostPhoto addPhoto(Long postId, Long userId, FileUpload file) throws IOException {
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            throw new IllegalArgumentException("You not author");
        }
        var result = mediaService.uploadPostPhoto(post, Files.newInputStream(file.uploadedFile()));

        var photo = new PostPhoto();
        photo.post = post;
        photo.url = result.url();
        photo.thumbnailUrl = result.thumbnailUrl();
        photo.position = (int) postPhotoRepo.countByPost(postId);
        postPhotoRepo.persist(photo);

        return photo;
    }

    @Transactional
    public void deletePhoto(Long postId, Long userId, Long photoId) {
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            throw new IllegalArgumentException("You not author");
        }

        var photo = postPhotoRepo.findByIdOptional(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        if (photo.url != null) mediaService.deletePhoto(photo.url);
        mediaService.deletePhoto(photo.thumbnailUrl);
        postPhotoRepo.delete(photo);
    }
}
