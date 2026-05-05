package org.critiqal.api.strava;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.strava.response.StravaActivity;
import org.critiqal.domain.strava.service.StravaServiceImpl;

import java.net.URI;
import java.util.List;
import java.util.Map;

/*
    StravaController
 */

@Path("/api/integrations/strava")
@Produces(MediaType.APPLICATION_JSON)
public class StravaResource {

    private final StravaServiceImpl stravaService;
    private final CurrentUser currentUser;

    public StravaResource(StravaServiceImpl stravaService,
                          CurrentUser currentUser) {
        this.stravaService = stravaService;
        this.currentUser = currentUser;
    }

    // create OAuth link for connect account
    @GET
    @Path("/connect")
    @Authenticated
    public Response connect() {
        var userId = currentUser.id();
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
    public Response disconnect() {
        stravaService.disconnect(currentUser.id());
        return Response.noContent().build();
    }

    @GET
    @Authenticated
    public Response getConnection() {
        return stravaService.getConnection(currentUser.id())
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.noContent().build());
    }

    // Retrieves recently user activities (default 5)
    @GET
    @Path("/activities")
    @Authenticated
    public List<StravaActivity> getActivities(@QueryParam("limit") @DefaultValue("5") int limit) {
        return stravaService.getRecentActivities(currentUser.id(), Math.min(limit, 20));
    }

    // Return public user account
    @GET
    @Path("/public/{userId}")
    public Response getPublicConnection(@PathParam("userId") Long userId) {
        return stravaService.getConnection(userId)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.noContent().build());
    }

}
