package org.critiqal.domain.auth.totp.repository;

import org.critiqal.domain.auth.totp.TotpSecret;

import java.util.Optional;
import java.util.UUID;

public interface TotpSecretRepository {
    Optional<TotpSecret> findByUserId(UUID userId);
    TotpSecret save(TotpSecret totpSecret);
    void deleteByUserId(UUID userId);
}
