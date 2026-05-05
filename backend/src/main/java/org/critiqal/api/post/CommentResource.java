package org.critiqal.api.post;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.api.post.request.AddCommentRequest;
import org.critiqal.api.post.response.CommentDTO;
import org.critiqal.domain.comment.service.CommentService;

import java.util.List;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

    private final CommentService commentService;

    public CommentResource(CommentService commentService) {
        this.commentService = commentService;
    }

    @POST
    @Path("/{id}/comments")
    @Authenticated
    public Response addComment(@Context SecurityContext ctx,
                               @PathParam("id") Long postId,
                               AddCommentRequest req) {
        var comment = commentService.addComment(extractUserId(ctx), postId, req.content());
        return  Response.status(Response.Status.CREATED)
                .entity(CommentDTO.from(comment))
                .build();
    }

    @GET
    @Path("/{id}/comments/{commentId}/replies")
    public List<CommentDTO> getReplies(@PathParam("id") Long postId,
                                       @PathParam("commentId") Long commentId) {
        return commentService.getReplies(postId, commentId).stream()
                .map(CommentDTO::from)
                .toList();
    }

    @POST
    @Path("/{id}/comments/{commentId}/replies")
    @Authenticated
    public Response addReply(@Context SecurityContext ctx,
                             @PathParam("id") Long postId,
                             @PathParam("commentId") Long commentId,
                             AddCommentRequest req) {
        var reply = commentService.addReply(extractUserId(ctx), postId, commentId, req.content());
        return Response.status(Response.Status.CREATED)
                .entity(CommentDTO.from(reply))
                .build();
    }

    @GET
    @Path("/{id}/comments")
    public List<CommentDTO> getComments(@PathParam("id") Long postId) {
        return commentService.getRootComments(postId).stream()
                .map(CommentDTO::from)
                .toList();
    }


    @DELETE
    @Path("/{id}/comments/{commentId}")
    @Authenticated
    public Response deleteComment(@Context SecurityContext ctx,
                                  @PathParam("id") Long postId,
                                  @PathParam("commentId") Long commentId) {
        commentService.deleteComment(postId, commentId, extractUserId(ctx));
        return Response.noContent().build();
    }

    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }
}
