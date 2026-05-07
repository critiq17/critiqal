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
import java.util.UUID;

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
        var postId = uuid(10);
        var ownerId = uuid(1);
        var requesterId = uuid(2);
        var post = postOwnedBy(postId, ownerId);
        var file = mock(FileUpload.class);

        when(postService.getById(postId)).thenReturn(post);

        assertThrows(ForbiddenException.class, () -> service.addPhoto(postId, requesterId, file));
        verifyNoInteractions(mediaService, postPhotoRepo);
    }

    @Test
    void addPhotoUploadsContentAndSavesPosition() throws IOException {
        var postId = uuid(10);
        var ownerId = uuid(7);
        var post = postOwnedBy(postId, ownerId);
        var file = createUpload("photo.png", "image/png", "png".getBytes());

        when(postService.getById(postId)).thenReturn(post);
        when(mediaService.uploadPostPhoto(eq(post), any(), eq("image/png"))).thenReturn("https://cdn/post.png");
        when(postPhotoRepo.countByPost(postId)).thenReturn(2L);
        when(postPhotoRepo.save(any(PostPhoto.class))).thenAnswer(invocation -> invocation.getArgument(0, PostPhoto.class));

        var saved = service.addPhoto(postId, ownerId, file);

        assertSame(post, saved.post);
        assertEquals("https://cdn/post.png", saved.url);
        assertEquals(2, saved.position);
        verify(postPhotoRepo).save(any(PostPhoto.class));
    }

    @Test
    void deletePhotoRejectsNonOwner() {
        var postId = uuid(10);
        var ownerId = uuid(1);
        var requesterId = uuid(2);
        var photoId = uuid(50);
        var post = postOwnedBy(postId, ownerId);

        when(postService.getById(postId)).thenReturn(post);

        assertThrows(ForbiddenException.class, () -> service.deletePhoto(postId, requesterId, photoId));
        verifyNoInteractions(postPhotoRepo, mediaService);
    }

    @Test
    void deletePhotoThrowsWhenPhotoMissing() {
        var postId = uuid(10);
        var ownerId = uuid(7);
        var photoId = uuid(50);
        var post = postOwnedBy(postId, ownerId);

        when(postService.getById(postId)).thenReturn(post);
        when(postPhotoRepo.findByIdOptional(photoId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deletePhoto(postId, ownerId, photoId));
    }

    @Test
    void deletePhotoThrowsWhenPhotoBelongsToAnotherPost() {
        var postId = uuid(10);
        var otherPostId = uuid(11);
        var ownerId = uuid(7);
        var photoId = uuid(50);
        var post = postOwnedBy(postId, ownerId);
        var photo = new PostPhoto();
        photo.post = postOwnedBy(otherPostId, ownerId);

        when(postService.getById(postId)).thenReturn(post);
        when(postPhotoRepo.findByIdOptional(photoId)).thenReturn(Optional.of(photo));

        assertThrows(NotFoundException.class, () -> service.deletePhoto(postId, ownerId, photoId));
        verify(mediaService, never()).deletePhoto(any());
        verify(postPhotoRepo, never()).delete(any(PostPhoto.class));
    }

    @Test
    void deletePhotoDeletesMediaWhenUrlPresent() {
        var postId = uuid(10);
        var ownerId = uuid(7);
        var photoId = uuid(50);
        var post = postOwnedBy(postId, ownerId);
        var photo = new PostPhoto();
        photo.post = post;
        photo.url = "https://cdn/post.png";

        when(postService.getById(postId)).thenReturn(post);
        when(postPhotoRepo.findByIdOptional(photoId)).thenReturn(Optional.of(photo));

        service.deletePhoto(postId, ownerId, photoId);

        verify(mediaService).deletePhoto("https://cdn/post.png");
        verify(postPhotoRepo).delete(photo);
    }

    @Test
    void deletePhotoSkipsMediaDeletionWhenUrlMissing() {
        var postId = uuid(10);
        var ownerId = uuid(7);
        var photoId = uuid(50);
        var post = postOwnedBy(postId, ownerId);
        var photo = new PostPhoto();
        photo.post = post;

        when(postService.getById(postId)).thenReturn(post);
        when(postPhotoRepo.findByIdOptional(photoId)).thenReturn(Optional.of(photo));

        service.deletePhoto(postId, ownerId, photoId);

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

    private static Post postOwnedBy(UUID postId, UUID authorId) {
        var author = new User();
        author.id = authorId;

        var post = new Post();
        post.id = postId;
        post.author = author;
        return post;
    }

    private static UUID uuid(int value) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(value));
    }
}
