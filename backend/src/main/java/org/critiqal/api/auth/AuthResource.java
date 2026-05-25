package org.critiqal.api.auth;

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
import org.critiqal.api.auth.response.SessionDto;
import org.critiqal.api.auth.response.TwoFactorChallengeResponse;
import org.critiqal.api.user.response.UserDTO;
import org.critiqal.domain.auth.email.service.EmailService;
import org.critiqal.domain.auth.session.SessionService;
import org.critiqal.domain.auth.session.repository.AuthSessionRepository;
import org.critiqal.domain.auth.totp.service.TotpService;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.infra.auth.AuthChallengeService;
import org.critiqal.infra.auth.metadata.DeviceGuard;
import org.critiqal.infra.auth.metadata.RequestMetadataResolver;
import org.critiqal.infra.auth.session.SessionFactoryCookie;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    private final UserService userService;
    private final SessionService sessions;
    private final SessionFactoryCookie cookies;
    private final CurrentUser currentUser;
    private final TotpService totpService;
    private final AuthChallengeService authChallengeService;
    private final DeviceGuard deviceGuard;
    private final RequestMetadataResolver metadataResolver;
    private final AuthSessionRepository authSessionRepo;
    private final EmailService emailService;

    public AuthResource(UserService userService,
                        SessionService sessions,
                        SessionFactoryCookie cookies,
                        CurrentUser currentUser,
                        TotpService totpService,
                        AuthChallengeService authChallengeService,
                        DeviceGuard deviceGuard,
                        RequestMetadataResolver metadataResolver,
                        AuthSessionRepository authSessionRepo,
                        EmailService emailService) {
        this.userService = userService;
        this.sessions = sessions;
        this.cookies = cookies;
        this.currentUser = currentUser;
        this.totpService = totpService;
        this.authChallengeService = authChallengeService;
        this.deviceGuard = deviceGuard;
        this.metadataResolver = metadataResolver;
        this.authSessionRepo = authSessionRepo;
        this.emailService = emailService;
    }

    @POST @Path("/register")
    public Response register(RegisterRequest req) {
        var meta = metadataResolver.resolve();

        deviceGuard.assertCanRegister(meta.deviceIdHash());

        var user = userService.register(Username.of(req.username()), req.password());
        var sid = sessions.create(user.id);
        return Response.status(201)
                .entity(UserDTO.from(user))
                .cookie(cookies.issue(sid))
                .build();
    }

    /**
     * 200 - login success (cookie)
     * 202 - required 2FA {challengeToken, method}
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

        sendNewDeviceAlertIfNeeded(user.id, user.email, user.emailVerified);

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
        sendNewDeviceAlertIfNeeded(userId, user.email, user.emailVerified);
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

    @GET @Path("/sessions") @Authenticated
    public Response listSessions(@Context HttpHeaders headers) {
        var cookie = headers.getCookies().get(cookies.name());
        var currentSid = cookie != null ? cookie.getValue() : null;
        var currentHash = RequestMetadataResolver.hash(currentSid);

        List<SessionDto> dtos = sessions.getSessions(currentUser.id())
                .stream()
                .map(session -> SessionDto.from(session, Objects.equals(session.sessionIdHash, currentHash)))
                .toList();

        return Response.ok(dtos).build();
    }

    @DELETE @Path("/sessions/{id}") @Consumes(MediaType.WILDCARD) @Authenticated
    public Response revokeSession(@PathParam("id") UUID sessionId) {
        if (!sessions.revoke(currentUser.id(), sessionId)) {
            return Response.status(403).build();
        }
        return Response.noContent().build();
    }

    private void sendNewDeviceAlertIfNeeded(
            UUID userId, String email, boolean emailVerified) {
        if (email == null || !emailVerified) return;

        var meta = metadataResolver.resolve();
        if (meta.deviceIdHash() == null) return;

        if (authSessionRepo.existsByDeviceIdHashAndUserId(meta.deviceIdHash(), userId)) return;

        try {
            emailService.sendSecurityAlert(
                    email,
                    "New login detected - Critiqal",
                    LoginAlertFormatter.buildMessage(meta)
            );
        } catch (Exception ignored) {
        }
    }
}
