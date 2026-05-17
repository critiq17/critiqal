package org.critiqal.api.auth;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.auth.request.ConfirmTotpRequest;
import org.critiqal.api.auth.response.TotpSetupResponse;
import org.critiqal.domain.auth.totp.service.TotpService;

import java.util.Map;

@Path("/api/auth/2fa")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class TwoFactorResource {

    private final TotpService  totpService;
    private final CurrentUser currentUser;

    public TwoFactorResource(TotpService totpService, CurrentUser currentUser) {
        this.totpService = totpService;
        this.currentUser = currentUser;
    }

    @POST @Path("/setup")
    public Response setup() {
        var r = totpService.setup(currentUser.id());
        return Response.ok(new TotpSetupResponse(r.qrCodeUri(), r.secret(), r.recoveryCodes())).build();
    }

    @POST @Path("/confirm") @Consumes(MediaType.APPLICATION_JSON)
    public Response confirm(ConfirmTotpRequest req) {
        totpService.confirm(currentUser.id(), req.code());
        return Response.noContent().build();
    }

    @DELETE @Consumes(MediaType.APPLICATION_JSON)
    public Response disable(ConfirmTotpRequest req) {
        totpService.disable(currentUser.id(), req.code());
        return Response.noContent().build();
    }

    @GET @Path("/status")
    public Response status() {
        return Response.ok(Map.of("enabled", totpService.isEnabled(currentUser.id()))).build();
    }
}
