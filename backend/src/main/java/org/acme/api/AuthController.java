package org.acme.api;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.acme.api.dtos.LoginRequest;
import org.acme.api.dtos.RegisterRequest;
import org.acme.api.dtos.UserDTO;
import org.acme.application.auth.SessionService;
import org.acme.domain.user.UserService;
import org.acme.infra.auth.SessionFactoryCookie;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {


    private final UserService userService;
    private final SessionService sessions;
    private final SessionFactoryCookie cookies;

    public AuthController(UserService userService,
                          SessionService sessions,
                          SessionFactoryCookie cookies) {
        this.userService = userService;
        this.sessions = sessions;
        this.cookies = cookies;
    }

    @POST @Path("/register")
    public Response register(RegisterRequest req) {
        var user = userService.register(req.username(), req.password());
        var sid = sessions.create(user.id);
        return Response.status(Response.Status.CREATED)
                .entity(UserDTO.from(user))
                .cookie(cookies.issue(sid))
                .build();
    }

    @POST @Path("/login")
    public Response login(LoginRequest req) {
        if (!userService.checkPassword(req.username(), req.password())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        var user = userService.getByUsername(req.username());
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
}
