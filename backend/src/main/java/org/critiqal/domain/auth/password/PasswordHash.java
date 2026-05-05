package org.critiqal.domain.auth.password;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Defines password hashing and verification operations.
 * Implementations encapsulate the hashing algorithm used by authentication.
 */
public interface PasswordHash {
    public String hash(String rawPassword);
    public boolean verify(String rawPassword, String hash);
}
