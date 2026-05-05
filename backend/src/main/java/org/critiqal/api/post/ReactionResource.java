package org.critiqal.api.post;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.post.request.ReactionRequest;
import org.critiqal.domain.reaction.ReactionType;
import org.critiqal.domain.reaction.service.ReactionService;

import java.util.Map;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactionResource {

    private final ReactionService reactionService;
    private final CurrentUser currentUser;

    public ReactionResource(ReactionService reactionService,
                            CurrentUser currentUser) {
        this.reactionService = reactionService;
        this.currentUser = currentUser;
    }
    @GET
    @Path("/{id}/reactions")
    public Map<ReactionType, Long> getReactions(@PathParam("id") Long postId) {
        return reactionService.getReactions(postId);
    }

    @GET
    @Path("/{id}/reactions/mine")
    @Authenticated
    public Response getMyReaction(@PathParam("id") Long postId) {
        Long userId = currentUser.id();
        return reactionService.getMyReaction(postId, userId)
                .map(type -> Response.ok(type).build())
                .orElse(Response.noContent().build());
    }

    @POST
    @Path("/{id}/reactions")
    @Authenticated
    public Response react(@PathParam("id") Long postId,
                          ReactionRequest req) {
        reactionService.react(currentUser.id(), postId, req.type());
        return Response.ok().build();
    }


    @DELETE
    @Path("/{id}/reactions")
    @Authenticated
    public Response removeReaction(@PathParam("id") Long postId) {
        reactionService.removeReaction(currentUser.id(), postId);
        return Response.noContent().build();
    }
}
