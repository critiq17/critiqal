package org.acme.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.api.dtos.AuthResponse;
import org.acme.api.dtos.LoginRequest;
import org.acme.api.dtos.RegisterRequest;
import org.acme.api.dtos.UserDTO;
import org.acme.domain.user.UserService;
import org.acme.utils.JwtTokenService;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    UserService userService;

    @Inject
    JwtTokenService jwtTokenService;


    @POST
    @Path("/register")
    public Response register(RegisterRequest req) {
        var user = userService.register(req.username(), req.password());
        var token = jwtTokenService.generate(user);
        return Response.status(Response.Status.CREATED)
                .entity(new AuthResponse(token, UserDTO.from(user)))
                .build();
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest req) {

        if (!userService.checkPassword(req.username(), req.password())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        var user = userService.getByUsername(req.username());
        var token = jwtTokenService.generate(user);
        return Response.ok(new AuthResponse(token, UserDTO.from(user))).build();
    }
}
