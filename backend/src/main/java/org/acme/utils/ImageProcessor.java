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
                .size(500, 500)
                .crop(Positions.CENTER)
                .outputFormat("jpeg")
                .outputQuality(0.90)
                .toOutputStream(output);
        return output.toByteArray();
    }

    public byte[] processPostPhoto(InputStream input) throws IOException {
        var output = new ByteArrayOutputStream();
        Thumbnails.of(input)
                .width(1600)
                .outputFormat("jpeg")
                .outputQuality(0.88)
                .toOutputStream(output);
        return output.toByteArray();
    }

    public byte[] processThumbnail(InputStream input) throws IOException {
        var output = new ByteArrayOutputStream();
        Thumbnails.of(input)
                .size(800, 800)
                .crop(Positions.CENTER)
                .outputFormat("jpeg")
                .outputQuality(0.82)
                .toOutputStream(output);
        return output.toByteArray();
    }
}
