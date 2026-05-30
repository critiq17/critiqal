package org.critiqal.api.admin;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.auth.request.LoginRequest;
import org.critiqal.api.auth.request.TwoFactorVerifyRequest;
import org.critiqal.api.auth.response.TwoFactorChallengeResponse;
import org.critiqal.domain.auth.totp.service.TotpService;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.infra.auth.AuthChallengeService;
import org.critiqal.infra.auth.admin.AdminAllowList;
import org.critiqal.infra.auth.admin.AdminCookieFactory;
import org.critiqal.infra.auth.admin.AdminSessionService;

import java.util.Map;

@Path("/api/admin/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminAuthResource {

    private final UserService userService;
    private final TotpService totpService;
    private final AuthChallengeService challenges;
    private final AdminAllowList allowList;
    private final AdminSessionService adminSessios;
    private final AdminCookieFactory cookies;

    public AdminAuthResource(UserService userService,
                             TotpService totpService,
                             AuthChallengeService challenges,
                             AdminAllowList allowList,
                             AdminSessionService adminSessios,
                             AdminCookieFactory cookies) {
        this.userService = userService;
        this.totpService = totpService;
        this.challenges = challenges;
        this.allowList = allowList;
        this.adminSessios = adminSessios;
        this.cookies = cookies;
    }

    @POST @Path("/login")
    public Response login(LoginRequest req) {
        var username = Username.of(req.username());
        if (!userService.checkPassword(username, req.password())) {
            throw new NotFoundException();
        }
        var user = userService.getByUsername(username);
        if (!allowList.contains(user.id)) {
            throw new NotFoundException();
        }

        if (!user.twoFactorEnabled) {
            throw new NotFoundException();
        }
        return Response.status(202)
                .entity(new TwoFactorChallengeResponse(challenges.create(user.id), "TOTP"))
                .build();
    }

    @POST @Path("/2fa")
    public Response verify(TwoFactorVerifyRequest req) {
        var userId = challenges.consume(req.challengeToken()).orElse(null);
        if (userId == null || !allowList.contains(userId)) {
            throw new NotFoundException();
        }
        try {
            totpService.verify(userId, req.code());
        } catch (DomainException e) {
            return Response.status(401).entity(Map.of("error", "Invalid code")).build();
        }
        var sid = adminSessios.create(userId);
        return Response.ok(Map.of("ok", true)).cookie(cookies.issue(sid)).build();
    }

    @POST @Path("/logout") @Consumes(MediaType.WILDCARD)
    public Response logout(@Context HttpHeaders h) {
        var c = h.getCookies().get(cookies.name());
        adminSessios.destroy(c != null ? c.getValue() : null);
        return Response.noContent().cookie(cookies.expire()).build();
    }
}
