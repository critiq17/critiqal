package org.acme.utils;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.swing.*;

@ApplicationScoped
public class PasswordHash {

    public String hash(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword);
    }

    public boolean verify(String rawPassword, String hash) {
        return BcryptUtil.matches(rawPassword, hash);
    }
}
