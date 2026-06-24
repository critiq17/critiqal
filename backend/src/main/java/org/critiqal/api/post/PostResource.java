package org.critiqal.api.post;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.post.request.CreatePostRequest;
import org.critiqal.api.post.response.PostDTO;
import org.critiqal.api.security.RequireVerifiedEmail;
import org.critiqal.domain.like.service.PostLikeServiceImpl;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.shared.pagination.PageRequest;

import java.util.UUID;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    private final PostService postService;
    private final CurrentUser currentUser;
    private final PostLikeServiceImpl postLikeService;
    private final PostEnricher postEnricher;

    public PostResource(PostService postService,
                        CurrentUser currentUser,
                        PostLikeServiceImpl postLikeService,
                        PostEnricher postEnricher) {
        this.postService = postService;
        this.currentUser = currentUser;
        this.postLikeService = postLikeService;
        this.postEnricher = postEnricher;
    }

    @GET
    public Page<PostDTO> getFeed(@BeanParam PageRequest pageRequest) {
        var page = postService.getLatestFeed(pageRequest.page(), pageRequest.size());
        return postEnricher.enrichWithLikes(page);
    }

    @GET
    @Path("/search")
    public Page<PostDTO> search(
            @QueryParam("q") String query,
            @BeanParam PageRequest pageRequest) {
        var page = postService.search(query, pageRequest.page(), pageRequest.size());
        return postEnricher.enrichWithLikes(page);
    }

    @GET
    @Path("/{id}")
    public PostDTO getPost(@PathParam("id") UUID id) {
        UUID userId = currentUser.idOrNull();
        postService.view(id, userId);
        var post = postService.getById(id);
        boolean likedByMe = userId != null && postLikeService.isLiked(id, userId);
        return PostDTO.from(post, post.likeCount, likedByMe);
    }

    @POST
    @Authenticated
    @RequireVerifiedEmail
    public Response createPost(CreatePostRequest req) {
        UUID authorId = currentUser.id();
        var post = postService.createPost(authorId, req.content());
        return Response.status(Response.Status.CREATED)
                .entity(PostDTO.from(post, 0L, false))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Authenticated
    @RequireVerifiedEmail
    public Response deletePost(@PathParam("id") UUID id) {
        postService.deletePostWithMedia(id, currentUser.id());
        return Response.noContent().build();
    }
}
