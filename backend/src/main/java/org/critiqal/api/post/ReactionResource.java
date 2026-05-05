package org.critiqal.api.post;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.api.post.request.ReactionRequest;
import org.critiqal.domain.reaction.ReactionType;
import org.critiqal.domain.reaction.service.ReactionService;

import java.util.Map;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactionResource {

    private final ReactionService reactionService;

    public ReactionResource(ReactionService reactionService) {
        this.reactionService = reactionService;
    }
    @GET
    @Path("/{id}/reactions")
    public Map<ReactionType, Long> getReactions(@PathParam("id") Long postId) {
        return reactionService.getReactions(postId);
    }

    @GET
    @Path("/{id}/reactions/mine")
    @Authenticated
    public Response getMyReaction(@Context SecurityContext ctx, @PathParam("id") Long postId) {
        Long userId = extractUserId(ctx);
        return reactionService.getMyReaction(postId, userId)
                .map(type -> Response.ok(type).build())
                .orElse(Response.noContent().build());
    }

    @POST
    @Path("/{id}/reactions")
    @Authenticated
    public Response react(@Context SecurityContext ctx,
                          @PathParam("id") Long postId,
                          ReactionRequest req) {
        reactionService.react(extractUserId(ctx), postId, req.type());
        return Response.ok().build();
    }


    @DELETE
    @Path("/{id}/reactions")
    @Authenticated
    public Response removeReaction(@Context SecurityContext ctx, @PathParam("id") Long postId) {
        reactionService.removeReaction(extractUserId(ctx), postId);
        return Response.noContent().build();
    }

    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }
}
