package org.critiqal.infra.auth.password;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.auth.password.PasswordHash;

@ApplicationScoped
public class BcryptPasswordHasher implements PasswordHash {
    public String hash(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword);
    }
    public boolean verify(String rawPassword, String hash) {
        return BcryptUtil.matches(rawPassword, hash);
    }
}
