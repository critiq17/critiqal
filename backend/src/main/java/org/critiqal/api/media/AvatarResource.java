package org.critiqal.api.media;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.domain.media.service.MediaService;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.service.UserService;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

@Path("/api/media")
public class AvatarResource {

    private final MediaService mediaService;
    private final UserService userService;

    public AvatarResource(MediaService mediaService, UserService userService) {
        this.mediaService = mediaService;
        this.userService = userService;
    }

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
