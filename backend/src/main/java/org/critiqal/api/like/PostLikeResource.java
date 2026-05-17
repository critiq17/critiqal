package org.critiqal.api.like;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.like.response.LikeResponse;
import org.critiqal.domain.like.service.PostLikeServiceImpl;

import java.util.UUID;

@Path("/api/posts/{postId}/likes")
@Produces(MediaType.APPLICATION_JSON)
public class PostLikeResource {

    private final PostLikeServiceImpl likeService;
    private final CurrentUser currentUser;

    public PostLikeResource(PostLikeServiceImpl likeService, CurrentUser currentUser) {
        this.likeService = likeService;
        this.currentUser = currentUser;
    }

    @POST
    @Authenticated
    public Response toggle(@PathParam("postId") UUID postId) {
        var result = likeService.toggle(postId, currentUser.id());
        return Response.ok(new LikeResponse(result.liked(), result.count())).build();
    }

    @GET
    public Response getLikes(@PathParam("postId") UUID postId) {
        long count = likeService.count(postId);
        UUID userId = currentUser.idOrNull();
        boolean likedByMe = userId != null && likeService.isLiked(postId, userId);
        return Response.ok(new LikeResponse(likedByMe, count)).build();
    }
}
