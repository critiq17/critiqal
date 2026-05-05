package org.critiqal.api.post;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.api.post.request.AddCommentRequest;
import org.critiqal.api.post.request.CreatePostRequest;
import org.critiqal.api.post.request.ReactionRequest;
import org.critiqal.api.post.response.CommentDTO;
import org.critiqal.api.post.response.PostDTO;
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
public class PostResource {

    private final PostService postService;
    private final MediaService mediaService;

    public PostResource(PostService postService,
                        MediaService mediaService) {
        this.postService = postService;
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
    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }
}
