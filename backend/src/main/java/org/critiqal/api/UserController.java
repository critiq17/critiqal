package org.critiqal.api;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.api.dtos.PageRequest;
import org.critiqal.api.dtos.PageResponse;
import org.critiqal.api.dtos.PostDTO;
import org.critiqal.api.dtos.UpdateProfileRequest;
import org.critiqal.api.dtos.UserDTO;
import org.critiqal.domain.follow.FollowService;
import org.critiqal.domain.post.PostService;
import org.critiqal.domain.user.UserService;

import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    UserService userService;

    @Inject
    FollowService followService;

    @Inject
    PostService postService;

    @GET
    @Path("/{username}")
    public UserDTO getProfile(@PathParam("username") String username) {
        return UserDTO.from(userService.getByUsername(username));
    }

    @GET
    @Path("/search")
    public List<UserDTO> search(@QueryParam("q") String query) {
        return userService.search(query).stream()
                .map(UserDTO::from)
                .toList();
    }

    @PUT
    @Path("/me")
    @Authenticated
    public UserDTO updateProfile(@Context SecurityContext ctx, UpdateProfileRequest req) {
        Long userId = extractUserId(ctx);
        return UserDTO.from(userService.updateProfile(userId, req.name(), req.bio()));
    }

    @POST
    @Path("/{id}/follow")
    @Authenticated
    @Consumes(MediaType.WILDCARD)
    public Response follow(@Context SecurityContext ctx, @PathParam("id") Long targetId) {
        Long followerId = extractUserId(ctx);
        followService.follow(followerId, targetId);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/follow")
    @Authenticated
    public Response unfollow(@Context SecurityContext ctx, @PathParam("id") Long targetId) {
        Long followerId = extractUserId(ctx);
        followService.unfollow(followerId, targetId);
        return Response.noContent().build();
    }


    // JAX-RS prefers literal paths over templates — these must appear before /{username}.
    // They are the "following feed" (subscriptions), reachable at /api/users/notifications/*.

    @GET
    @Path("/notifications")
    @Authenticated
    public Response getNotificationsInfo(@Context SecurityContext ctx) {
        Long userId = extractUserId(ctx);
        var stats = followService.getStats(userId);
        return Response.ok(stats).build();
    }

    @GET
    @Path("/notifications/posts")
    @Authenticated
    public PageResponse<PostDTO> getFollowingFeed(
            @Context SecurityContext ctx,
            @BeanParam PageRequest pageRequest) {
        Long userId = extractUserId(ctx);
        return postService.getFollowingFeed(userId, pageRequest.page(), pageRequest.size())
                .map(PostDTO::from);
    }

    @GET
    @Path("/{id}/followers")
    public List<UserDTO> getFollowers(@PathParam("id") Long userId) {
        return followService.getFollowers(userId).stream()
                .map(UserDTO::from)
                .toList();
    }

    @GET
    @Path("/{id}/following")
    public List<UserDTO> getFollowing(@PathParam("id") Long userId) {
        return followService.getFollowing(userId).stream()
                .map(UserDTO::from)
                .toList();
    }

    @GET
    @Path("/{username}/posts")
    public PageResponse<PostDTO> getUserPosts(
            @PathParam("username") String username,
            @BeanParam PageRequest pageRequest) {
        var user = userService.getByUsername(username);
        return postService.getUserPost(user.id, pageRequest.page(), pageRequest.size())
                .map(PostDTO::from);
    }

    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }

}
