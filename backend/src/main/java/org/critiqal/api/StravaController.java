package org.critiqal.api;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.api.dtos.strava.StravaActivityDTO;
import org.critiqal.application.strava.StravaService;

import java.net.URI;
import java.util.List;
import java.util.Map;

/*
    StravaController
 */

@Path("/api/integrations/strava")
@Produces(MediaType.APPLICATION_JSON)
public class StravaController {

    private final StravaService stravaService;

    public StravaController(StravaService stravaService) {
        this.stravaService = stravaService;
    }

    // create OAuth link for connect account
    @GET
    @Path("/connect")
    @Authenticated
    public Response connect(@Context SecurityContext ctx) {
        var userId = extractUserId(ctx);
        var url = stravaService.getAuthorizationUrl(userId);
        return Response.ok(Map.of("url", url)).build();
    }

    @GET
    @Path("/callback")
    public Response callback(
            @QueryParam("code") String code,
            @QueryParam("state") String state,
            @QueryParam("error") String error
    ) {
        if (error != null) {
            return Response.temporaryRedirect(
                    URI.create("/settings?strava=denied")
            ).build();
        }
        var userId = Long.parseLong(state);
        stravaService.handleCallback(userId, code);

        return Response.temporaryRedirect(
                URI.create("/settings?strava=connected")
        ).build();
    }

    // Disconnected strava account
    @DELETE
    @Authenticated
    public Response disconnect(@Context SecurityContext ctx) {
        stravaService.disconnect(extractUserId(ctx));
        return Response.noContent().build();
    }

    @GET
    @Authenticated
    public Response getConnection(@Context SecurityContext ctx) {
        return stravaService.getConnection(extractUserId(ctx))
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.noContent().build());
    }

    // Retrieves recently user activities (default 5)
    @GET
    @Path("/activities")
    @Authenticated
    public List<StravaActivityDTO> getActivities(
            @Context SecurityContext ctx,
            @QueryParam("limit") @DefaultValue("5") int limit
    ) {
        return stravaService.getRecentActivities(extractUserId(ctx), Math.min(limit, 20));
    }

    // Return public user account
    @GET
    @Path("/public/{userId}")
    public Response getPublicConnection(@PathParam("userId") Long userId) {
        return stravaService.getConnection(userId)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.noContent().build());
    }

    private Long extractUserId(SecurityContext ctx) {
        return Long.parseLong(ctx.getUserPrincipal().getName());
    }
}
