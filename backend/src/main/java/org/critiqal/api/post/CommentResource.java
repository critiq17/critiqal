package org.critiqal.api.post;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.post.request.AddCommentRequest;
import org.critiqal.api.post.response.CommentDTO;
import org.critiqal.api.security.RequireVerifiedEmail;
import org.critiqal.domain.comment.Comment;
import org.critiqal.domain.comment.service.CommentService;
import org.critiqal.domain.like.service.CommentLikeServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

    private final CommentService commentService;
    private final CommentLikeServiceImpl commentLikeService;
    private final CurrentUser currentUser;

    public CommentResource(CommentService commentService,
                           CommentLikeServiceImpl commentLikeService,
                           CurrentUser currentUser) {
        this.commentService = commentService;
        this.commentLikeService = commentLikeService;
        this.currentUser = currentUser;
    }

    @POST
    @Path("/{id}/comments")
    @Authenticated
    @RequireVerifiedEmail
    public Response addComment(@PathParam("id") UUID postId,
                               AddCommentRequest req) {
        var comment = commentService.addComment(currentUser.id(), postId, req.content());
        return Response.status(Response.Status.CREATED)
                .entity(toDto(comment))
                .build();
    }

    @GET
    @Path("/{id}/comments/{commentId}/replies")
    public List<CommentDTO> getReplies(@PathParam("id") UUID postId,
                                       @PathParam("commentId") UUID commentId) {
        return enrich(commentService.getReplies(postId, commentId));
    }

    @POST
    @Path("/{id}/comments/{commentId}/replies")
    @Authenticated
    @RequireVerifiedEmail
    public Response addReply(@PathParam("id") UUID postId,
                             @PathParam("commentId") UUID commentId,
                             AddCommentRequest req) {
        var reply = commentService.addReply(currentUser.id(), postId, commentId, req.content());
        return Response.status(Response.Status.CREATED)
                .entity(toDto(reply))
                .build();
    }

    @GET
    @Path("/{id}/comments")
    public List<CommentDTO> getComments(@PathParam("id") UUID postId) {
        return enrich(commentService.getRootComments(postId));
    }

    @DELETE
    @Path("/{id}/comments/{commentId}")
    @Authenticated
    public Response deleteComment(@PathParam("id") UUID postId,
                                  @PathParam("commentId") UUID commentId) {
        commentService.deleteComment(postId, commentId, currentUser.id());
        return Response.noContent().build();
    }

    // A freshly created comment/reply has no likes yet.
    private CommentDTO toDto(Comment comment) {
        return CommentDTO.from(comment, 0L, false);
    }

    // Batch-load like counts and the caller's like state in two queries.
    private List<CommentDTO> enrich(List<Comment> comments) {
        if (comments.isEmpty()) return List.of();

        var ids = comments.stream().map(c -> c.id).toList();
        Map<UUID, Long> counts = commentLikeService.countByCommentIds(ids);
        UUID userId = currentUser.idOrNull();
        Set<UUID> liked = userId != null
                ? commentLikeService.likedCommentIds(userId, ids)
                : Set.of();

        return comments.stream()
                .map(c -> CommentDTO.from(c, counts.getOrDefault(c.id, 0L), liked.contains(c.id)))
                .toList();
    }
}
