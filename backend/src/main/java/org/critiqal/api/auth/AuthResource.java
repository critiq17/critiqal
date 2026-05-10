package org.critiqal.api.auth;

import com.fasterxml.jackson.core.util.RecyclerPool;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.auth.request.LoginRequest;
import org.critiqal.api.auth.request.RegisterRequest;
import org.critiqal.api.auth.request.TwoFactorVerifyRequest;
import org.critiqal.api.auth.response.TwoFactorChallengeResponse;
import org.critiqal.api.user.response.UserDTO;
import org.critiqal.domain.auth.session.SessionService;
import org.critiqal.domain.auth.totp.service.TotpService;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.infra.auth.AuthChallengeService;
import org.critiqal.infra.auth.session.SessionFactoryCookie;

import java.util.Map;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {


    private final UserService userService;
    private final SessionService sessions;
    private final SessionFactoryCookie cookies;
    private final CurrentUser currentUser;
    private final TotpService totpService;
    private final AuthChallengeService  authChallengeService;

    public AuthResource(UserService userService,
                        SessionService sessions,
                        SessionFactoryCookie cookies,
                        CurrentUser currentUser,
                        TotpService totpService,
                        AuthChallengeService authChallengeService) {
        this.userService = userService;
        this.sessions = sessions;
        this.cookies = cookies;
        this.currentUser = currentUser;
        this.totpService = totpService;
        this.authChallengeService = authChallengeService;
    }

    @POST @Path("/register")
    public Response register(RegisterRequest req) {
        var user = userService.register(Username.of(req.username()), req.password());
        var sid = sessions.create(user.id);
        return Response.status(201).entity(UserDTO.from(user)).cookie(cookies.issue(sid)).build();
    }

    /**
     * 200 - login success (cookie)
     * 202 - required 2FA {challengeToke, method}
     * 401 - incorrect data
     */
    @POST @Path("/login")
    public Response login(LoginRequest req) {
        var username = Username.of(req.username());
        if (!userService.checkPassword(username, req.password())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        var user = userService.getByUsername(username);
        if (user.twoFactorEnabled) {
            return Response.status(202)
                    .entity(new TwoFactorChallengeResponse(authChallengeService.create(user.id), "TOTP"))
                    .build();
        }
        return Response.ok(UserDTO.from(user)).cookie(cookies.issue(sessions.create(user.id))).build();
    }

    @POST @Path("/login/2fa")
    public Response loginWith2fa(TwoFactorVerifyRequest req) {
        var userId = authChallengeService.consume(req.challengeToken()).orElse(null);
        if (userId == null)
            return Response.status(401).entity(Map.of("error", "Invalid or expired challenge")).build();

        try {
            totpService.verify(userId, req.code());
        } catch (DomainException e) {
            return Response.status(401).entity(Map.of("error", e.getMessage())).build();
        }

        var user = userService.getById(userId);
        return Response.ok(UserDTO.from(user)).cookie(cookies.issue(sessions.create(userId))).build();
    }

    @POST @Path("/logout") @Consumes(MediaType.WILDCARD) @Authenticated
    public Response logout(@Context HttpHeaders headers) {
        var cookie = headers.getCookies().get(cookies.name());
        sessions.destroy(cookie != null ? cookie.getValue() : null);
        return Response.noContent().cookie(cookies.expire()).build();
    }

    @GET @Path("/me") @Authenticated
    public Response me() {
        return Response.ok(UserDTO.from(userService.getById(currentUser.id()))).build();
    }

    @DELETE @Path("/sessions/{id}") @Consumes(MediaType.WILDCARD) @Authenticated
    public Response revokeSession(@PathParam("id") String sessionId) {
        var owner = sessions.resolve(sessionId);
        if (owner.isEmpty() || !owner.get().equals(currentUser.id()))
            return Response.status(403).build();
        sessions.destroy(sessionId);
        return Response.noContent().build();
    }
}
