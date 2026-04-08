package org.acme.utils;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domain.user.User;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class JwtTokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public String generate(User user) {
        return Jwt.issuer(issuer)
                .subject(user.id.toString())
                .claim("username", user.username)
                .expiresIn(Duration.ofDays(7))
                .sign();
    }
}
