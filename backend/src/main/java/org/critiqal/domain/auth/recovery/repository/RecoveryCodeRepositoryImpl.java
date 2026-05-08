package org.critiqal.domain.auth.recovery.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.auth.recovery.RecoveryCode;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RecoveryCodeRepositoryImpl implements RecoveryCodeRepository, PanacheRepository<RecoveryCode> {

    @Override
    public List<RecoveryCode> findActiveByUserId(UUID userId) {
        return find("user.id = ?1 AND usedAt IS NULL", userId).list();
    }

    @Override
    @Transactional
    public void saveAll(List<RecoveryCode> codes) {
        codes.forEach(this::persist);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        delete("user.id = ?1", userId);
    }

    @Override
    public long countActiveByUserId(UUID userId) {
        return count("user.id = ?1 AND usedAt IS NULL", userId);
    }
}
