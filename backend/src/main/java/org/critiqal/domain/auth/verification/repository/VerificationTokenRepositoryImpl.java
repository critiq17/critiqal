package org.critiqal.domain.auth.verification.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.auth.verification.VerificationToken;
import org.critiqal.domain.auth.verification.VerificationTokenType;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VerificationTokenRepositoryImpl implements VerificationTokenRepository, PanacheRepository<VerificationToken> {

    @Override
    public Optional<VerificationToken> findByTokenHashAndType(String tokenHash, VerificationTokenType type) {
        return find("tokenHash = ?1 AND type = ?2", tokenHash, type).firstResultOptional();
    }

    @Override
    @Transactional
    public void save(VerificationToken token) {
        persist(token);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndType(UUID userId, VerificationTokenType type) {
        delete("user.id = ?1 AND type = ?2", userId, type);
    }
}
