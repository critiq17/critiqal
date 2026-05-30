package org.critiqal.api.admin;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.admin.response.AdminBadgeDTO;
import org.critiqal.api.admin.response.AdminUserDTO;
import org.critiqal.api.post.response.PostDTO;
import org.critiqal.domain.badge.Badge;
import org.critiqal.domain.badge.BadgeCode;
import org.critiqal.domain.badge.UserBadge;
import org.critiqal.domain.badge.service.BadgeService;
import org.critiqal.domain.post.service.PostService;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.shared.pagination.PageRequest;
import org.critiqal.domain.user.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/admin")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    private final AdminUserQueryService userQuery;
    private final BadgeService badgeService;
    private final PostService postService;

    public AdminResource(AdminUserQueryService userQuery,
                         BadgeService badgeService,
                         PostService postService) {
        this.userQuery = userQuery;
        this.badgeService = badgeService;
        this.postService = postService;
    }

    @GET @Path("/me")
    public Response me() {
        return Response.ok(Map.of("admin", true)).build();
    }

    @GET @Path("/users/search")
    public Page<AdminUserDTO> searchUsers(@QueryParam("q") String query,
                                          @BeanParam PageRequest pageRequest) {
        return userQuery.search(query, pageRequest.page(), pageRequest.size());
    }

    @GET @Path("/users/{userId}")
    public AdminUserDTO getUser(@PathParam("userId") UUID userId) {
        return userQuery.getUser(userId);
    }

    @GET @Path("/posts/search")
    public Page<PostDTO> searchPosts(@QueryParam("q") String query,
                                     @BeanParam PageRequest pageRequest) {
        var page = (query == null || query.isBlank())
                ? postService.getLatestFeed(pageRequest.page(), pageRequest.size())
                : postService.search(query, pageRequest.page(), pageRequest.size());
        // Admin view does not need viewer-specific like state.
        return page.map(post -> PostDTO.from(post, 0L, false));
    }

    @GET @Path("/badges")
    public List<AdminBadgeDTO> listBadges() {
        return badgeService.listAll().stream().map(AdminBadgeDTO::from).toList();
    }

    @POST @Path("/users/{userId}/badges") @Transactional
    public Response grantBadge(@PathParam("userId") UUID userId, Map<String, String> body) {
        var code = body.get("code");
        var user = User.<User>findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        var badge = Badge.<Badge>find("code", code).firstResultOptional()
                .orElseThrow(() -> new NotFoundException("badge: " + code));

        boolean already = UserBadge.count("user = ?1 and badge = ?2", user, badge) > 0;
        if (!already) {
            var ub = new UserBadge();
            ub.user = user;
            ub.badge = badge;
            ub.persist();
        }
        return Response.ok(Map.of("granted", code, "user", userId.toString())).build();
    }

    @DELETE @Path("/users/{userId}/badges/{code}")
    public Response revokeBadge(@PathParam("userId") UUID userId, @PathParam("code") String code) {
        BadgeCode badgeCode;
        try {
            badgeCode = BadgeCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("badge: " + code);
        }
        boolean removed = badgeService.revokeBadge(userId, badgeCode);
        return Response.ok(Map.of("revoked", code, "user", userId.toString(), "removed", removed)).build();
    }
}
