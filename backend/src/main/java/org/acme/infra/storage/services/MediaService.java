package org.acme.infra.storage.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.post.Post;
import org.acme.domain.post_photo.PostPhotoRepository;
import org.acme.infra.storage.s3.R2StorageService;
import org.acme.utils.ImageProcessor;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class MediaService {

    @Inject R2StorageService r2;
    @Inject ImageProcessor imageProcessor;
    @Inject PostPhotoRepository postPhotoRepo;

    public record UploadResult(String url, String thumbnailUrl) {}

    public String uploadAvatar(Long userId, InputStream imageStream) throws IOException {
        var processed = imageProcessor.processAvatar(imageStream);
        var key = "avatars/" + userId + "/" + UUID.randomUUID() + ".jpg";
        return r2.upload(key, processed, "image/jpeg");
    }

    public UploadResult uploadPostPhoto(Post post, InputStream imageStream) throws IOException {

        if (postPhotoRepo.countByPost(post.id) >= 3) {
            throw new IllegalArgumentException("Max 3 photos per post");
        }
        var rawBytes = imageStream.readAllBytes();
        var uid  = UUID.randomUUID().toString();

        var fullProcessed = imageProcessor.processPostPhoto(new ByteArrayInputStream(rawBytes));
        var thumbProcessed = imageProcessor.processThumbnail(new ByteArrayInputStream(rawBytes));

        var fullKey = "posts/" + post.id + "/" + uid + ".jpg";
        var thumbKey = "posts/" + post.id + "/" + uid + "_thumb.jpg";

        return new UploadResult(
                r2.upload(fullKey, fullProcessed, "image/jpeg"),
                r2.upload(thumbKey, thumbProcessed, "image/jpeg")
        );
    }

    public void deleteAllPostPhotos(Long postId) {
        postPhotoRepo.findByPost(postId).forEach(photo -> {
            r2.deleteByUrl(photo.url);
            r2.deleteByUrl(photo.thumbnailUrl);
        });
        postPhotoRepo.deleteByPost(postId);
    }

    public void deletePhoto(String url) {
        r2.deleteByUrl(url);
    }
}
