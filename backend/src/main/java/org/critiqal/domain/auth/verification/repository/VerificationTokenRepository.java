package org.critiqal.domain.auth.verification.repository;

import org.critiqal.domain.auth.verification.VerificationToken;
import org.critiqal.domain.auth.verification.VerificationTokenType;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository {
    Optional<VerificationToken> findByTokenHashAndType(String tokenHash, VerificationTokenType type);
    void save(VerificationToken token);
    void deleteByUserIdAndType(UUID userId, VerificationTokenType type);
}
