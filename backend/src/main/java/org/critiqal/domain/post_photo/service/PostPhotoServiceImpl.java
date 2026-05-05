package org.critiqal.domain.post_photo.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.post_photo.PostPhoto;
import org.critiqal.domain.post_photo.repository.PostPhotoRepository;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.media.service.MediaService;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Default implementation of {@link PostPhotoService}.
 * Enforces post photo ownership, limits, and storage lifecycle.
 */
@ApplicationScoped
public class PostPhotoServiceImpl implements PostPhotoService {
    private final PostPhotoRepository postPhotoRepo;
    private final PostService postService;
    private final MediaService mediaService;

    public PostPhotoServiceImpl(PostPhotoRepository postPhotoRepo,
                            PostService postService,
                            MediaService mediaService) {
        this.postPhotoRepo = postPhotoRepo;
        this.postService = postService;
        this.mediaService = mediaService;
    }

    @Override
    @Transactional
    public PostPhoto addPhoto(Long postId, Long userId, FileUpload file) throws IOException {
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            throw new ForbiddenException("Not your post");
        }
        String url;
        try (var imageStream = Files.newInputStream(file.uploadedFile())) {
            url = mediaService.uploadPostPhoto(post, imageStream, file.contentType());
        }

        var photo = new PostPhoto();
        photo.post = post;
        photo.url = url;
        photo.position = (int) postPhotoRepo.countByPost(postId);
        return postPhotoRepo.save(photo);
    }

    @Override
    @Transactional
    public void deletePhoto(Long postId, Long userId, Long photoId) {
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            throw new ForbiddenException("Not your post");
        }

        var photo = postPhotoRepo.findByIdOptional(photoId)
                .orElseThrow(() -> new NotFoundException("Photo not found"));
        if (!photo.post.id.equals(postId)) {
            throw new NotFoundException("Photo not found");
        }

        if (photo.url != null) {
            mediaService.deletePhoto(photo.url);
        }
        postPhotoRepo.delete(photo);
    }
}
