package org.critiqal.api.media;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import org.critiqal.api.post.response.PostDTO;
import org.critiqal.domain.post_photo.service.PostPhotoService;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.domain.media.service.MediaService;
import org.jboss.resteasy.reactive.RestForm;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.ForbiddenException;

@Path("/api/media")
public class PostPhotoResource {

    private final MediaService mediaService;
    private final PostService postService;
    private final PostPhotoService postPhotoService;

    public PostPhotoResource(MediaService mediaService,
                             UserService userService,
                             PostService postService,
                             PostPhotoService postPhotoService) {
        this.mediaService = mediaService;
        this.postService = postService;
        this.postPhotoService = postPhotoService;
    }

    @POST
    @Path("/posts/{postId}/photos")
    @Authenticated
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addPostPhoto(
            @Context SecurityContext ctx,
            @PathParam("postId") Long postId,
            @RestForm("file") FileUpload file
    ) throws IOException {
        validateImage(file);
        Long userId = extractUserId(ctx);
        var photo = postPhotoService.addPhoto(postId, userId, file);

        return Response.status(Response.Status.CREATED).entity(PostDTO.PostPhotoDTO.from(photo)).build();
    }

    @DELETE
    @Path("/posts/{postId}/photos/{photoId}")
    @Authenticated
    @Transactional
    public Response deletePostPhoto(
            @Context SecurityContext ctx,
            @PathParam("postId") Long postId,
            @PathParam("photoId") Long photoId
    ) {
        Long userId = extractUserId(ctx);
        postPhotoService.deletePhoto(postId, userId, photoId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/posts/{postId}/photos")
    @Authenticated
    public Response deleteAllPostPhotos(
            @Context SecurityContext ctx,
            @PathParam("postId") Long postId
    ) {
        Long userId = extractUserId(ctx);
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            throw new ForbiddenException("Not your post");
        }
        mediaService.deleteAllPostPhotos(postId);
        return Response.noContent().build();
    }

    private void validateImage(FileUpload file) {
        var allowed = Set.of("image/jpeg", "image/png", "image/webp", "image/heic");
        if (!allowed.contains(file.contentType())) {
            throw new DomainException("Only images are allowed");
        }
        if (file.size() > 10 * 1024 * 1024) {
            throw new DomainException("Max file size is 10MB");
        }
    }

    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }
}
