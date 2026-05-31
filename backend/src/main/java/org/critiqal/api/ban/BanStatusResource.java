package org.critiqal.api.ban;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.domain.ban.service.BanService;

import java.util.Map;

@Path("/api/ban-status")
@Produces(MediaType.APPLICATION_JSON)
public class BanStatusResource {

    private final BanService banService;
    private final CurrentUser currentUser;

    public BanStatusResource(BanService banService,
                             CurrentUser currentUser) {
        this.banService = banService;
        this.currentUser = currentUser;
    }

    @GET
    public Response status() {
        var userId = currentUser.idOrNull();
        if (userId == null) return Response.ok(Map.of("banned", false)).build();
        return banService.activeBan(userId)
                .<Response>map(b -> Response.ok(Map.of(
                        "banned", true,
                        "reason", b.reason,
                        "expiresAt", b.expiresAt == null ? "" : b.expiresAt.toString()
                )).build())
                .orElse(Response.ok(Map.of("banned", false)).build());
    }
}
