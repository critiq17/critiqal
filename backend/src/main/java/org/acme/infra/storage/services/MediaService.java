package org.acme.infra.storage.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.infra.storage.s3.R2StorageService;
import org.acme.utils.ImageProcessor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@ApplicationScoped
public class MediaService {

    @Inject
    R2StorageService r2;

    @Inject
    ImageProcessor imageProcessor;

    public record UploadResult(String url, String thumbnailUrl) {}

    public String uploadAvatar(Long userId, InputStream imageStream) throws IOException {
        var processed = imageProcessor.processAvatar(imageStream);
        var key = "avatars/" + userId + "/" + UUID.randomUUID() + ".jpg";
        return r2.upload(key, processed, "image/jpeg");
    }

    public UploadResult uploadPostPhoto(Long postId, InputStream imageStream) throws IOException {

        var rawBytes = imageStream.readAllBytes();

        var fullProcessed = imageProcessor.processPostPhoto(
                new ByteArrayInputStream(rawBytes)
        );
        var thumbProcessed = imageProcessor.processThumbnail(
                new ByteArrayInputStream(rawBytes)
        );

        var uid = UUID.randomUUID().toString();
        var fullKey = "posts/" + postId + "/" + uid + ".jpg";
        var thumbKey = "posts/" + postId + "/" + uid + "_thumb.jpg";

        var fullUrl = r2.upload(fullKey, fullProcessed, "image/jpeg");
        var thumbUrl = r2.upload(thumbKey, thumbProcessed, "image/jpeg");

        return new UploadResult(fullUrl, thumbUrl);
    }

    public void deletePhoto(String url) {
        r2.deleteByUrl(url);
    }
}
