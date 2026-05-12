package org.critiqal.domain.auth.recovery.repository;

import org.critiqal.domain.auth.recovery.RecoveryCode;

import java.util.List;
import java.util.UUID;

public interface RecoveryCodeRepository {
    List<RecoveryCode> findActiveByUserId(UUID userId);
    void saveAll(List<RecoveryCode> codes);
    void deleteByUserId(UUID userId);
    long countActiveByUserId(UUID userId);
}
