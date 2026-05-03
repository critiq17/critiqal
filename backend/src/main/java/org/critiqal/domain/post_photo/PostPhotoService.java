package org.critiqal.domain.post_photo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.critiqal.domain.post.PostService;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.infra.storage.services.MediaService;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

@ApplicationScoped
public class PostPhotoService {

    @Inject PostPhotoRepository postPhotoRepo;
    @Inject PostService postService;
    @Inject MediaService mediaService;

    @Transactional
    public PostPhoto addPhoto(Long postId, Long userId, FileUpload file) throws IOException {
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            throw new ForbiddenException("You not author");
        }
        var url = mediaService.uploadPostPhoto(post, Files.newInputStream(file.uploadedFile()), file.contentType());

        var photo = new PostPhoto();
        photo.post = post;
        photo.url = url;
        photo.position = (int) postPhotoRepo.countByPost(postId);
        postPhotoRepo.persist(photo);

        return photo;
    }

    @Transactional
    public void deletePhoto(Long postId, Long userId, Long photoId) {
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            throw new ForbiddenException("You not author");
        }

        var photo = postPhotoRepo.findByIdOptional(photoId)
                .orElseThrow(() -> new NotFoundException("Photo not found"));

        if (photo.url != null) mediaService.deletePhoto(photo.url);
        postPhotoRepo.delete(photo);
    }
}
