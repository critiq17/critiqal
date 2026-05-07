package org.critiqal.api.user;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.shared.pagination.PageRequest;
import org.critiqal.api.post.response.PostDTO;
import org.critiqal.api.user.request.UpdateProfileRequest;
import org.critiqal.api.user.response.UserDTO;
import org.critiqal.domain.follow.service.FollowService;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.service.UserService;

import java.util.List;
import java.util.UUID;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserService userService;
    private final FollowService followService;
    private final PostService postService;
    private final CurrentUser currentUser;

    public UserResource(UserService userService,
                        FollowService followService,
                        PostService postService,
                        CurrentUser currentUser) {
        this.userService = userService;
        this.followService = followService;
        this.postService = postService;
        this.currentUser = currentUser;
    }

    @GET
    @Path("/{username}")
    public UserDTO getProfile(@PathParam("username") String username) {
        return UserDTO.from(userService.getByUsername(Username.of(username)));
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
    public UserDTO updateProfile(UpdateProfileRequest req) {
        UUID userId = currentUser.id();
        return UserDTO.from(userService.updateProfile(userId, req.name(), req.bio()));
    }

    @POST
    @Path("/{id}/follow")
    @Authenticated
    @Consumes(MediaType.WILDCARD)
    public Response follow(@PathParam("id") UUID targetId) {
        UUID followerId = currentUser.id();
        followService.follow(followerId, targetId);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/follow")
    @Authenticated
    public Response unfollow(@PathParam("id") UUID targetId) {
        UUID followerId = currentUser.id();
        followService.unfollow(followerId, targetId);
        return Response.noContent().build();
    }


    // JAX-RS prefers literal paths over templates — these must appear before /{username}.
    // They are the "following feed" (subscriptions), reachable at /api/users/notifications/*.

    @GET
    @Path("/notifications")
    @Authenticated
    public Response getNotificationsInfo() {
        UUID userId = currentUser.id();
        var stats = followService.getStats(userId);
        return Response.ok(stats).build();
    }

    @GET
    @Path("/notifications/posts")
    @Authenticated
    public Page<PostDTO> getFollowingFeed(@BeanParam PageRequest pageRequest) {
        UUID userId = currentUser.id();
        return postService.getFollowingFeed(userId, pageRequest.page(), pageRequest.size())
                .map(PostDTO::from);
    }

    @GET
    @Path("/{id}/followers")
    public List<UserDTO> getFollowers(@PathParam("id") UUID userId) {
        return followService.getFollowers(userId).stream()
                .map(UserDTO::from)
                .toList();
    }

    @GET
    @Path("/{id}/following")
    public List<UserDTO> getFollowing(@PathParam("id") UUID userId) {
        return followService.getFollowing(userId).stream()
                .map(UserDTO::from)
                .toList();
    }

    @GET
    @Path("/{username}/posts")
    public Page<PostDTO> getUserPosts(
            @PathParam("username") String username,
            @BeanParam PageRequest pageRequest) {
        var user = userService.getByUsername(Username.of(username));
        return postService.getUserPost(user.id, pageRequest.page(), pageRequest.size())
                .map(PostDTO::from);
    }
}
