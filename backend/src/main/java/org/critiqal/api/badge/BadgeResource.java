package org.critiqal.api.badge;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.badge.request.AwardBadgeRequest;
import org.critiqal.domain.badge.service.BadgeService;

import java.util.UUID;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class BadgeResource {

    private final BadgeService badgeService;
    private final CurrentUser currentUser;

    public BadgeResource(BadgeService badgeService,
                         CurrentUser currentUser) {
        this.badgeService = badgeService;
        this.currentUser = currentUser;
    }

    @GET
    @Path("/users/{id}/badges")
    public Response getUserBadges(@PathParam("id")UUID userId) {
        return Response.ok(badgeService.getUserBadges(userId)).build();
    }

    @POST
    @Path("/users/{id}/badges")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    public Response awardBadge(@PathParam("id") UUID userId,
                               AwardBadgeRequest req) {
        badgeService.awardBadge(userId, req.code(), req.metadata());
        return Response.status(201).build();
    }
}
