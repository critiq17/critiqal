package org.acme.api;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.api.dtos.UpdateProfileRequest;
import org.acme.api.dtos.UserDTO;
import org.acme.domain.follow.FollowService;
import org.acme.domain.user.UserService;

import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    UserService userService;

    @Inject
    FollowService followService;

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
        return UserDTO.from(userService.updateProfile(userId, req.name(), req.bio(), req.avatarUrl()));
    }

    @POST
    @Path("/{id}/follow")
    @Authenticated
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


    @GET
    @Path("/{id}/followers")
    @Authenticated
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

    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }

}
