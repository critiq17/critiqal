package org.acme.utils;

import jakarta.enterprise.context.ApplicationScoped;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class ImageProcessor {

    public byte[] processAvatar(InputStream input) throws IOException {
        var output = new ByteArrayOutputStream();
        Thumbnails.of(input)
                .size(400, 400)
                .crop(Positions.CENTER)
                .outputFormat("jpeg")
                .outputQuality(0.85)
                .toOutputStream(output);
        return output.toByteArray();
    }

    public byte[] processPostPhoto(InputStream input) throws IOException {
        var output = new ByteArrayOutputStream();
        Thumbnails.of(input)
                .width(1200)
                .outputFormat("jpeg")
                .outputQuality(0.82)
                .toOutputStream(output);
        return output.toByteArray();
    }

    public byte[] processThumbnail(InputStream input) throws IOException {
        var output = new ByteArrayOutputStream();
        Thumbnails.of(input)
                .size(600, 600)
                .crop(Positions.CENTER)
                .outputFormat("jpeg")
                .outputQuality(0.75)
                .toOutputStream(output);
        return output.toByteArray();
    }
}
