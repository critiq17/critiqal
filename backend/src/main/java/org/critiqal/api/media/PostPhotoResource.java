package org.critiqal.api.media;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.transaction.Transactional;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.post.response.PostDTO;
import org.critiqal.api.security.RequireVerifiedEmail;
import org.critiqal.domain.media.service.MediaService;
import org.critiqal.domain.post_photo.service.PostPhotoService;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.infra.security.ImageValidator;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.util.UUID;

@Path("/api/media")
@RequireVerifiedEmail
public class PostPhotoResource {

    private final MediaService mediaService;
    private final PostService postService;
    private final PostPhotoService postPhotoService;
    private final CurrentUser currentUser;

    public PostPhotoResource(MediaService mediaService,
                             PostService postService,
                             PostPhotoService postPhotoService,
                             CurrentUser currentUser) {
        this.mediaService = mediaService;
        this.postService = postService;
        this.postPhotoService = postPhotoService;
        this.currentUser = currentUser;
    }

    @POST
    @Path("/posts/{postId}/photos")
    @Authenticated
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addPostPhoto(
            @PathParam("postId") UUID postId,
            @RestForm("file") FileUpload file
    ) throws IOException {
        validateImage(file);
        UUID userId = currentUser.id();
        var photo = postPhotoService.addPhoto(postId, userId, file);

        return Response.status(Response.Status.CREATED).entity(PostDTO.PostPhotoDTO.from(photo)).build();
    }

    @DELETE
    @Path("/posts/{postId}/photos/{photoId}")
    @Authenticated
    @Transactional
    public Response deletePostPhoto(
            @PathParam("postId") UUID postId,
            @PathParam("photoId") UUID photoId
    ) {
        UUID userId = currentUser.id();
        postPhotoService.deletePhoto(postId, userId, photoId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/posts/{postId}/photos")
    @Authenticated
    public Response deleteAllPostPhotos(
            @PathParam("postId") UUID postId
    ) {
        UUID userId = currentUser.id();
        var post = postService.getById(postId);

        if (!post.author.id.equals(userId)) {
            throw new ForbiddenException("Not your post");
        }
        mediaService.deleteAllPostPhotos(postId);
        return Response.noContent().build();
    }

    private void validateImage(FileUpload file) throws IOException {
        ImageValidator.validate(file);
    }

}
