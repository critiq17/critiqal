package org.critiqal.api;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.api.dtos.*;
import org.critiqal.domain.comment.service.CommentService;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.reaction.service.ReactionService;
import org.critiqal.domain.reaction.ReactionType;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.shared.pagination.PageRequest;
import org.critiqal.domain.media.service.MediaService;

import java.util.List;
import java.util.Map;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final ReactionService reactionService;
    private final MediaService mediaService;

    public PostController(PostService postService,
                          CommentService commentService,
                          ReactionService reactionService,
                          MediaService mediaService) {
        this.postService = postService;
        this.commentService = commentService;
        this.reactionService = reactionService;
        this.mediaService = mediaService;
    }

    @GET
    public Page<PostDTO> getFeed(@BeanParam PageRequest pageRequest) {
        return postService.getLatestFeed(pageRequest.page(), pageRequest.size())
                .map(PostDTO::from);
    }

    @GET
    @Path("/search")
    public Page<PostDTO> search(
            @QueryParam("q") String query,
            @BeanParam PageRequest pageRequest) {
        return postService.search(query, pageRequest.page(), pageRequest.size())
                .map(PostDTO::from);
    }

    @GET
    @Path("/{id}")
    public PostDTO getPost(@Context SecurityContext ctx, @PathParam("id") Long id) {
        Long userId = ctx.getUserPrincipal() != null
                ? Long.parseLong(ctx.getUserPrincipal().getName())
                : null;
        postService.view(id, userId);
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
        mediaService.deleteAllPostPhotos(id);
        return Response.noContent().build();
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
