package org.critiqal.domain.auth.totp.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.auth.totp.TotpSecret;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TotpSecretRepositoryImpl implements TotpSecretRepository, PanacheRepository<TotpSecret> {

    @Override
    public Optional<TotpSecret> findByUserId(UUID userId) {
        return find("user.id = ?1", userId).firstResultOptional();
    }

    @Override
    @Transactional
    public TotpSecret save(TotpSecret totpSecret) {
        persist(totpSecret);
        return totpSecret;
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        delete("user.id = ?1", userId);
    }
}

