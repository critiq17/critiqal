package org.critiqal.domain.ban.repository;

import org.critiqal.domain.ban.UserBan;

import java.util.Optional;
import java.util.UUID;

public interface UserBanRepository {
    Optional<UserBan> findActiveByUserId(UUID userId);
    UserBan save(UserBan ban);
}
