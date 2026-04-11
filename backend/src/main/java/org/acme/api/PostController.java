package org.acme.api;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.api.dtos.*;
import org.acme.domain.comment.CommentService;
import org.acme.domain.post.PostService;
import org.acme.domain.reaction.ReactionService;
import org.acme.domain.reaction.ReactionType;

import java.util.List;
import java.util.Map;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostController {

    @Inject
    PostService postService;

    @Inject
    CommentService commentService;

    @Inject
    ReactionService reactionService;

    @GET
    public List<PostDTO> getFeed() {
        return postService.getLatestFeed().stream()
                .map(PostDTO::from)
                .toList();
    }

    @GET
    @Path("/search")
    public List<PostDTO> search(@QueryParam("q") String query) {
        return postService.search(query).stream()
                .map(PostDTO::from)
                .toList();
    }

    @GET
    @Path("/{id}")
    public PostDTO getPost(@PathParam("id") Long id) {
        postService.view(id);
        return PostDTO.from(postService.getById(id));
    }

    @POST
    @Authenticated
    public Response createPost(@Context SecurityContext ctx, CreatePostRequest req) {
        Long authorId = extractUserId(ctx);
        var post = postService.createPost(authorId, req.content());
        return Response.status(Response.Status.CREATED)
                .entity(PostDTO.from(post))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Authenticated
    public Response deletePost(@Context SecurityContext ctx, @PathParam("id") Long id) {
        postService.deletePost(id, extractUserId(ctx));
        return Response.noContent().build();
    }


/*
    @GET
    @Path("/{id}/comments")
    public List<CommentDTO> getComments(@PathParam("id") Long postId) {
        return commentService.getPostComments(postId).stream()
                .map(CommentDTO::from)
                .toList();
    }
*/

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
        return commentService.getReplies(commentId).stream()
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
        commentService.deleteComment(commentId, extractUserId(ctx));
        return Response.noContent().build();
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
        reactionService.removeReact(extractUserId(ctx), postId);
        return Response.noContent().build();
    }

    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }
}

