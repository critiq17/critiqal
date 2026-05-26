package org.critiqal.api.like;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.like.response.LikeResponse;
import org.critiqal.api.security.RequireVerifiedEmail;
import org.critiqal.domain.like.service.CommentLikeServiceImpl;

import java.util.UUID;

@Path("/api/posts/{postId}/comments/{commentId}/likes")
@Produces(MediaType.APPLICATION_JSON)
public class CommentLikeResource {

    private final CommentLikeServiceImpl service;
    private final CurrentUser currentUser;

    public CommentLikeResource(CommentLikeServiceImpl service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @POST
    @Authenticated
    @RequireVerifiedEmail
    public Response toggle(@PathParam("postId") UUID postId,
                           @PathParam("commentId") UUID commentId) {
        var result = service.toggle(commentId, currentUser.id());
        return Response.ok(new LikeResponse(result.liked(), result.count())).build();
    }

    @GET
    public Response getLikes(@PathParam("postId") UUID postId,
                             @PathParam("commentId") UUID commentId) {
        long count = service.count(commentId);
        UUID userId = currentUser.idOrNull();
        boolean likedByMe = userId != null && service.isLiked(commentId, userId);
        return Response.ok(new LikeResponse(likedByMe, count)).build();
    }
}
