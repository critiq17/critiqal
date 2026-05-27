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
import org.critiqal.domain.auth.email.service.EmailVerificationService;
import org.critiqal.domain.auth.session.SessionService;
import org.critiqal.domain.auth.session.repository.AuthSessionRepository;
import org.critiqal.domain.auth.totp.service.TotpService;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.infra.auth.AuthChallengeService;
import org.critiqal.infra.auth.RateLimiter;
import org.critiqal.infra.auth.metadata.DeviceGuard;
import org.critiqal.infra.auth.metadata.RequestMetadataResolver;
import org.critiqal.infra.auth.session.SessionFactoryCookie;

import java.time.Duration;
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
    private final RateLimiter rateLimiter;
    private final EmailVerificationService verifyService;

    public AuthResource(UserService userService,
                        SessionService sessions,
                        SessionFactoryCookie cookies,
                        CurrentUser currentUser,
                        TotpService totpService,
                        AuthChallengeService authChallengeService,
                        DeviceGuard deviceGuard,
                        RequestMetadataResolver metadataResolver,
                        AuthSessionRepository authSessionRepo,
                        EmailService emailService, RateLimiter rateLimiter,
    EmailVerificationService verifyService) {
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
        this.rateLimiter = rateLimiter;
        this.verifyService = verifyService;
    }

    @POST @Path("/register")
    public Response register(RegisterRequest req) {
        var meta = metadataResolver.resolve();
        deviceGuard.assertCanRegister(meta.deviceIdHash());

        if (meta.ipHash() != null) {
            rateLimiter.check(RateLimiter.key("register-ip", meta.ipHash()), 5, Duration.ofHours(1));
        }
        // Validate email up front so a duplicate/invalid address doesn't leave an orphan user row.
        verifyService.assertEmailAvailable(req.email());
        var user = userService.register(Username.of(req.username()), req.password());

        verifyService.sendEmailVerification(user.id, req.email());
        var sid = sessions.create(user.id);
        return Response.status(201)
                .entity(UserDTO.from(user))
                .cookie(cookies.issue(sid))
                .build();
    }

    /**
     * 200 - login success (cookie) — only for accounts that don't yet have a verified email
     *       (frontend still gates them on the verify-email page).
     * 202 - required second factor {challengeToken, method=TOTP|EMAIL}
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

        // Verified email = mandatory email OTP as a second factor on every sign-in.
        // Without this, anyone with a leaked password could log in silently.
        if (user.emailVerified && user.email != null) {
            try {
                verifyService.sendLoginCode(user.id);
            } catch (DomainException e) {
                return Response.status(401).entity(Map.of("error", e.getMessage())).build();
            }
            return Response.status(202)
                    .entity(new TwoFactorChallengeResponse(authChallengeService.create(user.id), "EMAIL"))
                    .build();
        }

        // No verified email yet — issue session so the frontend can drive the
        // verify-email flow (existing UX) and emit a fresh code if one is pending.
        sendNewDeviceAlertIfNeeded(user.id, user.email, user.emailVerified);
        issueVerificationCodeIfPending(user);

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
        issueVerificationCodeIfPending(user);
        return Response.ok(UserDTO.from(user)).cookie(cookies.issue(sessions.create(userId))).build();
    }

    @POST @Path("/login/email")
    public Response loginWithEmail(TwoFactorVerifyRequest req) {
        var userId = authChallengeService.consume(req.challengeToken()).orElse(null);
        if (userId == null)
            return Response.status(401).entity(Map.of("error", "Invalid or expired challenge")).build();

        try {
            verifyService.verifyLoginCode(userId, req.code());
        } catch (DomainException e) {
            return Response.status(401).entity(Map.of("error", e.getMessage())).build();
        }

        var user = userService.getById(userId);
        sendNewDeviceAlertIfNeeded(userId, user.email, user.emailVerified);
        return Response.ok(UserDTO.from(user)).cookie(cookies.issue(sessions.create(userId))).build();
    }

    // After login, if the account has a pending email that wasn't verified yet,
    // send a fresh code so the user can finish the flow without re-registering.
    // Best-effort: a delivery hiccup must not break login.
    private void issueVerificationCodeIfPending(org.critiqal.domain.user.User user) {
        if (user.emailVerified || user.pendingEmail == null) return;
        try {
            verifyService.resendVerification(user.id);
        } catch (Exception ignored) {
        }
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
