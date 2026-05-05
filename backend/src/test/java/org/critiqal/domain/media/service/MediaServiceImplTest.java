package org.critiqal.domain.media.service;

import org.critiqal.domain.post.Post;
import org.critiqal.domain.post_photo.PostPhoto;
import org.critiqal.domain.post_photo.repository.PostPhotoRepository;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.infra.storage.image.ImageProcessor;
import org.critiqal.infra.storage.r2.R2StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MediaServiceImplTest {

    private final R2StorageService r2 = mock(R2StorageService.class);
    private final ImageProcessor imageProcessor = mock(ImageProcessor.class);
    private final PostPhotoRepository postPhotoRepo = mock(PostPhotoRepository.class);

    private MediaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MediaServiceImpl(r2, imageProcessor, postPhotoRepo);
    }

    @Test
    void uploadAvatarProcessesAndUploadsAsJpeg() throws IOException {
        when(imageProcessor.processAvatar(any())).thenReturn(new byte[]{9, 8, 7});
        when(r2.upload(
                argThat(key -> key.startsWith("avatars/5/") && key.endsWith(".jpg")),
                argThat(bytes -> Arrays.equals(bytes, new byte[]{9, 8, 7})),
                eq("image/jpeg")))
                .thenReturn("https://cdn/avatar.jpg");

        var url = service.uploadAvatar(5L, new ByteArrayInputStream(new byte[]{1, 2, 3}));

        assertEquals("https://cdn/avatar.jpg", url);
        verify(imageProcessor).processAvatar(any());
    }

    @Test
    void uploadPostPhotoRejectsWhenPostAlreadyHasThreePhotos() {
        var post = new Post();
        post.id = 9L;

        when(postPhotoRepo.countByPost(9L)).thenReturn(3L);

        assertThrows(DomainException.class,
                () -> service.uploadPostPhoto(post, new ByteArrayInputStream(new byte[]{1}), "image/png"));
        verifyNoInteractions(r2);
    }

    @Test
    void uploadPostPhotoUploadsWithDerivedExtension() throws IOException {
        var post = new Post();
        post.id = 9L;

        when(postPhotoRepo.countByPost(9L)).thenReturn(2L);
        when(r2.upload(
                argThat(key -> key.startsWith("posts/9/") && key.endsWith(".png")),
                argThat(bytes -> Arrays.equals(bytes, new byte[]{1, 2, 3})),
                eq("image/png")))
                .thenReturn("https://cdn/post.png");

        var url = service.uploadPostPhoto(post, new ByteArrayInputStream(new byte[]{1, 2, 3}), "image/png");

        assertEquals("https://cdn/post.png", url);
    }

    @Test
    void deleteAllPostPhotosRemovesRemoteFilesThenRepositoryRows() {
        var first = new PostPhoto();
        first.url = "https://cdn/1.jpg";
        var second = new PostPhoto();
        second.url = "https://cdn/2.jpg";

        when(postPhotoRepo.findByPost(12L)).thenReturn(List.of(first, second));

        service.deleteAllPostPhotos(12L);

        var inOrder = inOrder(r2, postPhotoRepo);
        inOrder.verify(r2).deleteByUrl("https://cdn/1.jpg");
        inOrder.verify(r2).deleteByUrl("https://cdn/2.jpg");
        inOrder.verify(postPhotoRepo).deleteByPost(12L);
    }

    @Test
    void deletePhotoDelegatesToStorage() {
        service.deletePhoto("https://cdn/photo.jpg");

        verify(r2).deleteByUrl("https://cdn/photo.jpg");
    }
}
