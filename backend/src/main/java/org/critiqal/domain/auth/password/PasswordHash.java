package org.critiqal.domain.auth.password;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

public interface PasswordHash {
    public String hash(String rawPassword);
    public boolean verify(String rawPassword, String hash);
}
