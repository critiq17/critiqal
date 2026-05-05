package org.critiqal.domain.post_photo.service;

import org.critiqal.domain.media.service.MediaService;
import org.critiqal.domain.post.Post;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.post_photo.PostPhoto;
import org.critiqal.domain.post_photo.repository.PostPhotoRepository;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.User;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostPhotoServiceImplTest {

    private final PostPhotoRepository postPhotoRepo = mock(PostPhotoRepository.class);
    private final PostService postService = mock(PostService.class);
    private final MediaService mediaService = mock(MediaService.class);

    private PostPhotoServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new PostPhotoServiceImpl(postPhotoRepo, postService, mediaService);
    }

    @Test
    void addPhotoRejectsNonOwner() {
        var post = postOwnedBy(10L, 1L);
        var file = mock(FileUpload.class);

        when(postService.getById(10L)).thenReturn(post);

        assertThrows(ForbiddenException.class, () -> service.addPhoto(10L, 2L, file));
        verifyNoInteractions(mediaService, postPhotoRepo);
    }

    @Test
    void addPhotoUploadsContentAndSavesPosition() throws IOException {
        var post = postOwnedBy(10L, 7L);
        var file = createUpload("photo.png", "image/png", "png".getBytes());

        when(postService.getById(10L)).thenReturn(post);
        when(mediaService.uploadPostPhoto(eq(post), any(), eq("image/png"))).thenReturn("https://cdn/post.png");
        when(postPhotoRepo.countByPost(10L)).thenReturn(2L);
        when(postPhotoRepo.save(any(PostPhoto.class))).thenAnswer(invocation -> invocation.getArgument(0, PostPhoto.class));

        var saved = service.addPhoto(10L, 7L, file);

        assertSame(post, saved.post);
        assertEquals("https://cdn/post.png", saved.url);
        assertEquals(2, saved.position);
        verify(postPhotoRepo).save(any(PostPhoto.class));
    }

    @Test
    void deletePhotoRejectsNonOwner() {
        var post = postOwnedBy(10L, 1L);

        when(postService.getById(10L)).thenReturn(post);

        assertThrows(ForbiddenException.class, () -> service.deletePhoto(10L, 2L, 50L));
        verifyNoInteractions(postPhotoRepo, mediaService);
    }

    @Test
    void deletePhotoThrowsWhenPhotoMissing() {
        var post = postOwnedBy(10L, 7L);

        when(postService.getById(10L)).thenReturn(post);
        when(postPhotoRepo.findByIdOptional(50L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deletePhoto(10L, 7L, 50L));
    }

    @Test
    void deletePhotoThrowsWhenPhotoBelongsToAnotherPost() {
        var post = postOwnedBy(10L, 7L);
        var photo = new PostPhoto();
        photo.post = postOwnedBy(11L, 7L);

        when(postService.getById(10L)).thenReturn(post);
        when(postPhotoRepo.findByIdOptional(50L)).thenReturn(Optional.of(photo));

        assertThrows(NotFoundException.class, () -> service.deletePhoto(10L, 7L, 50L));
        verify(mediaService, never()).deletePhoto(any());
        verify(postPhotoRepo, never()).delete(any(PostPhoto.class));
    }

    @Test
    void deletePhotoDeletesMediaWhenUrlPresent() {
        var post = postOwnedBy(10L, 7L);
        var photo = new PostPhoto();
        photo.post = post;
        photo.url = "https://cdn/post.png";

        when(postService.getById(10L)).thenReturn(post);
        when(postPhotoRepo.findByIdOptional(50L)).thenReturn(Optional.of(photo));

        service.deletePhoto(10L, 7L, 50L);

        verify(mediaService).deletePhoto("https://cdn/post.png");
        verify(postPhotoRepo).delete(photo);
    }

    @Test
    void deletePhotoSkipsMediaDeletionWhenUrlMissing() {
        var post = postOwnedBy(10L, 7L);
        var photo = new PostPhoto();
        photo.post = post;

        when(postService.getById(10L)).thenReturn(post);
        when(postPhotoRepo.findByIdOptional(50L)).thenReturn(Optional.of(photo));

        service.deletePhoto(10L, 7L, 50L);

        verify(mediaService, never()).deletePhoto(any());
        verify(postPhotoRepo).delete(photo);
    }

    private FileUpload createUpload(String filename, String contentType, byte[] bytes) throws IOException {
        var path = tempDir.resolve(filename);
        Files.write(path, bytes);

        var upload = mock(FileUpload.class);
        when(upload.uploadedFile()).thenReturn(path);
        when(upload.contentType()).thenReturn(contentType);
        return upload;
    }

    private static Post postOwnedBy(Long postId, Long authorId) {
        var author = new User();
        author.id = authorId;

        var post = new Post();
        post.id = postId;
        post.author = author;
        return post;
    }
}
