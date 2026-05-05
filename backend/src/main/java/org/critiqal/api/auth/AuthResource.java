package org.critiqal.api.auth;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.critiqal.api.auth.request.LoginRequest;
import org.critiqal.api.auth.request.RegisterRequest;
import org.critiqal.api.user.response.UserDTO;
import org.critiqal.domain.auth.session.SessionService;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.infra.auth.session.SessionFactoryCookie;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {


    private final UserService userService;
    private final SessionService sessions;
    private final SessionFactoryCookie cookies;

    public AuthResource(UserService userService,
                        SessionService sessions,
                        SessionFactoryCookie cookies) {
        this.userService = userService;
        this.sessions = sessions;
        this.cookies = cookies;
    }

    @POST @Path("/register")
    public Response register(RegisterRequest req) {
        var username = Username.of(req.username());
        var user = userService.register(username, req.password());
        var sid = sessions.create(user.id);
        return Response.status(Response.Status.CREATED)
                .entity(UserDTO.from(user))
                .cookie(cookies.issue(sid))
                .build();
    }

    @POST @Path("/login")
    public Response login(LoginRequest req) {
        var username = Username.of(req.username());

        if (!userService.checkPassword(username, req.password())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        var user = userService.getByUsername(username);
        var sid = sessions.create(user.id);
        return Response.ok(UserDTO.from(user))
                .cookie(cookies.issue(sid))
                .build();
    }

    @POST @Path("/logout")
    @Consumes(MediaType.WILDCARD)
    @Authenticated
    public Response logout(@CookieParam("session") String sid) {
        sessions.destroy(sid);
        return Response.noContent().cookie(cookies.expire()).build();
    }

    @GET @Path("/me")
    @Authenticated
    public Response me(@Context SecurityContext ctx) {
        var userId = Long.parseLong(ctx.getUserPrincipal().getName());
        return Response.ok(UserDTO.from(userService.getById(userId))).build();
    }

    @DELETE @Path("/sessions/{id}")
    @Consumes(MediaType.WILDCARD)
    @Authenticated
    public Response revokeSession(@PathParam("id") String sessionId,
                                  @Context SecurityContext ctx) {
        var callerId = Long.parseLong(ctx.getUserPrincipal().getName());
        var owner = sessions.resolve(sessionId);
        if (owner.isEmpty() || !owner.get().equals(callerId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        sessions.destroy(sessionId);
        return Response.noContent().build();
    }
}
