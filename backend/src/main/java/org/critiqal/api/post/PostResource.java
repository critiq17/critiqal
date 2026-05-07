package org.critiqal.api.post;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.post.request.CreatePostRequest;
import org.critiqal.api.post.response.PostDTO;
import org.critiqal.domain.media.service.MediaService;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.shared.pagination.PageRequest;

import java.util.UUID;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    private final PostService postService;
    private final MediaService mediaService;
    private final CurrentUser currentUser;

    public PostResource(PostService postService,
                        MediaService mediaService,
                        CurrentUser currentUser) {
        this.postService = postService;
        this.mediaService = mediaService;
        this.currentUser = currentUser;
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
    public PostDTO getPost(@PathParam("id") UUID id) {
        UUID userId = currentUser.idOrNull();
        postService.view(id, userId);
        return PostDTO.from(postService.getById(id));
    }

    @POST
    @Authenticated
    public Response createPost(CreatePostRequest req) {
        UUID authorId = currentUser.id();
        var post = postService.createPost(authorId, req.content());
        return Response.status(Response.Status.CREATED)
                .entity(PostDTO.from(post))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Authenticated
    public Response deletePost(@PathParam("id") UUID id) {
        postService.deletePost(id, currentUser.id());
        mediaService.deleteAllPostPhotos(id);
        return Response.noContent().build();
    }
}
