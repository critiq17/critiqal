package org.critiqal.api.auth;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.auth.request.PasswordResetRequest;
import org.critiqal.api.auth.request.RecoveryCodeRequest;
import org.critiqal.api.auth.request.SetEmailRequest;
import org.critiqal.api.auth.request.VerifyEmailRequest;
import org.critiqal.domain.auth.email.service.EmailVerificationService;
import org.critiqal.domain.auth.recovery.service.AccountRecoveryService;
import org.critiqal.domain.auth.session.SessionService;
import org.critiqal.infra.auth.RateLimiter;
import org.critiqal.infra.auth.session.SessionFactoryCookie;

import java.time.Duration;
import java.util.Map;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountRecoveryResource {

    private final AccountRecoveryService  recoveryService;
    private final EmailVerificationService verifyService;
    private final SessionService sessions;
    private final SessionFactoryCookie cookies;
    private final CurrentUser currentUser;
    private final RateLimiter rateLimiter;

    public AccountRecoveryResource(
            AccountRecoveryService recoveryService, EmailVerificationService verifyService,
            SessionService sessions, SessionFactoryCookie cookies,
            CurrentUser currentUser, RateLimiter rateLimiter
    ) {
        this.recoveryService = recoveryService;
        this.verifyService = verifyService;
        this.sessions = sessions;
        this.cookies = cookies;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
    }

    @POST @Path("/email") @Authenticated
    public Response setEmail(SetEmailRequest req) {
        verifyService.sendEmailVerification(currentUser.id(), req.email());
        return Response.ok(Map.of("message", "Verification email sent")).build();
    }

    @POST @Path("/email/verify")
    public Response verifyEmail(VerifyEmailRequest req) {
        verifyService.verify(req.token());
        return Response.ok(Map.of("message", "Email verified")).build();
    }

    @POST @Path("/recovery/request")
    public Response requestReset(SetEmailRequest req) {
        if (req.email() != null)
            rateLimiter.check(RateLimiter.key("pwd-reset", req.email().toLowerCase()), 3, Duration.ofHours(1));
        recoveryService.requestPasswordReset(req.email());
        return Response.ok(Map.of("message", "If the email exists, a reset link has been sent")).build();
    }

    @POST @Path("/recovery/reset")
    public Response resetPassword(PasswordResetRequest req) {
        recoveryService.resetPassword(req.token(), req.newPassword());
        return Response.noContent().build();
    }

    @POST @Path("/recovery/code")
    public Response useRecoveryCode(RecoveryCodeRequest req) {
        rateLimiter.check(RateLimiter.key("rec-code", req.username()), 5, Duration.ofMinutes(15));
        var userId = recoveryService.useRecoveryCode(req.username(), req.recoveryCode());
        return Response.ok().cookie(cookies.issue(sessions.create(userId))).build();
    }

    @GET @Path("/recovery/codes/count") @Authenticated
    public Response codesCount() {
        return Response.ok(Map.of("activeCount",
                recoveryService.countActiveRecoveryCodes(currentUser.id()))).build();
    }

    @POST @Path("/recovery/codes/regenerate") @Authenticated
    public Response regenerateCode() {
        var codes = recoveryService.regenerateRecoveryCodes(currentUser.id());
        return Response.ok(Map.of("codes", codes,
                "warning", "Store these safely - they won't be shown again.")).build();
    }
}
