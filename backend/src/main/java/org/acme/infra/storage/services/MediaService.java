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


    public String uploadAvatar(Long userId, InputStream imageStream) throws IOException {
        var processed = imageProcessor.processAvatar(imageStream);
        var key = "avatars/" + userId + "/" + UUID.randomUUID() + ".jpg";
        return r2.upload(key, processed, "image/jpeg");
    }

    public String uploadPostPhoto(Post post, InputStream imageStream, String contentType) throws IOException {
        if (postPhotoRepo.countByPost(post.id) >= 3) {
            throw new IllegalArgumentException("Max 3 photos per post");
        }
        var bytes = imageStream.readAllBytes();
        var ext = contentType.split("/")[1];
        var key = "posts/" + post.id + "/" + UUID.randomUUID() + "." + ext;
        return r2.upload(key, bytes, contentType);
    }

    public void deleteAllPostPhotos(Long postId) {
        postPhotoRepo.findByPost(postId).forEach(photo -> {
            r2.deleteByUrl(photo.url);
        });
        postPhotoRepo.deleteByPost(postId);
    }

    public void deletePhoto(String url) {
        r2.deleteByUrl(url);
    }
}
