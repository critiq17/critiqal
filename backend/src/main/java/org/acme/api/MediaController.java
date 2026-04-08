package org.acme.api;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.domain.post.PostService;
import org.acme.domain.user.UserService;
import org.acme.infra.storage.services.MediaService;
import org.jboss.resteasy.reactive.RestForm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

@Path("/api/media")
public class MediaController {

    @Inject
    MediaService mediaService;
    @Inject
    UserService userService;
    @Inject
    PostService postService;

    @POST
    @Path("/avatar")
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadAvatar(
            @Context SecurityContext ctx,
            @RestForm("file") FileUpload file
            ) throws IOException {
        validateImage(file);

        Long userId = extractUserId(ctx);
        var user = userService.getById(userId);

        if (user.avatarUrl != null) {
            mediaService.deletePhoto(user.avatarUrl);
        }

        var url = mediaService.uploadAvatar(userId, Files.newInputStream(file.uploadedFile()));
        userService.updateAvatar(userId, url);

        return Response.ok(Map.of("avatarUrl", url)).build();
    }

    @DELETE
    @Path("/avatar")
    @Authenticated
    public Response deleteAvatar(@Context SecurityContext ctx) {
        Long userId = extractUserId(ctx);
        var user = userService.getById(userId);

        if (user.avatarUrl != null) {
            mediaService.deletePhoto(user.avatarUrl);
            userService.updateAvatar(userId, null);
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/posts/{postId}/photo")
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadPostPhoto(
            @Context SecurityContext ctx,
            @PathParam("postId") Long postId,
            @RestForm("file") FileUpload file
    ) throws IOException {
        validateImage(file);

        Long userId = extractUserId(ctx);
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (post.photoUrl != null) {
            mediaService.deletePhoto(post.photoUrl);
            mediaService.deletePhoto(post.photoThumbnailUrl);
        }

        var result = mediaService.uploadPostPhoto(
                postId,
                Files.newInputStream(file.uploadedFile())
        );

        postService.updatePhoto(postId, result.url(), result.thumbnailUrl());

        return Response.ok(Map.of(
                "photoUrl", result.url(),
                "thumbnailUrl", result.thumbnailUrl()
        )).build();
    }

    @DELETE
    @Path("/posts/{postId}/photo")
    @Authenticated
    public Response deletePostPhoto(
            @Context SecurityContext ctx,
            @PathParam("postId") Long postId
    ) {
        Long userId = extractUserId(ctx);
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        mediaService.deletePhoto(post.photoUrl);
        mediaService.deletePhoto(post.photoThumbnailUrl);
        postService.updatePhoto(postId, null, null);

        return Response.noContent().build();
    }

    private void validateImage(FileUpload file) {
        var allowed = Set.of("image/jpeg", "image/png", "image/webp", "image/heic");
        if (!allowed.contains(file.contentType())) {
            throw new IllegalArgumentException("Only images are allowed");
        }
        if (file.size() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Max file size is 10MB");
        }
    }

    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }
}
