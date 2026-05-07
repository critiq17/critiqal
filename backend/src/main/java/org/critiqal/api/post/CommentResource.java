package org.critiqal.api.post;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.post.request.AddCommentRequest;
import org.critiqal.api.post.response.CommentDTO;
import org.critiqal.domain.comment.service.CommentService;

import java.util.List;
import java.util.UUID;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

    private final CommentService commentService;
    private final CurrentUser currentUser;

    public CommentResource(CommentService commentService,
                           CurrentUser currentUser) {
        this.commentService = commentService;
        this.currentUser = currentUser;
    }

    @POST
    @Path("/{id}/comments")
    @Authenticated
    public Response addComment(@PathParam("id") UUID postId,
                               AddCommentRequest req) {
        var comment = commentService.addComment(currentUser.id(), postId, req.content());
        return  Response.status(Response.Status.CREATED)
                .entity(CommentDTO.from(comment))
                .build();
    }

    @GET
    @Path("/{id}/comments/{commentId}/replies")
    public List<CommentDTO> getReplies(@PathParam("id") UUID postId,
                                       @PathParam("commentId") UUID commentId) {
        return commentService.getReplies(postId, commentId).stream()
                .map(CommentDTO::from)
                .toList();
    }

    @POST
    @Path("/{id}/comments/{commentId}/replies")
    @Authenticated
    public Response addReply(@PathParam("id") UUID postId,
                             @PathParam("commentId") UUID commentId,
                             AddCommentRequest req) {
        var reply = commentService.addReply(currentUser.id(), postId, commentId, req.content());
        return Response.status(Response.Status.CREATED)
                .entity(CommentDTO.from(reply))
                .build();
    }

    @GET
    @Path("/{id}/comments")
    public List<CommentDTO> getComments(@PathParam("id") UUID postId) {
        return commentService.getRootComments(postId).stream()
                .map(CommentDTO::from)
                .toList();
    }


    @DELETE
    @Path("/{id}/comments/{commentId}")
    @Authenticated
    public Response deleteComment(@PathParam("id") UUID postId,
                                  @PathParam("commentId") UUID commentId) {
        commentService.deleteComment(postId, commentId, currentUser.id());
        return Response.noContent().build();
    }
}
