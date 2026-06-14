package org.critiqal.domain.activity.repository;

import org.critiqal.domain.activity.UserActivityStats;

import java.util.Optional;
import java.util.UUID;

public interface UserActivityStatsRepository {

    UserActivityStats findOrCreate(UUID userId);

    Optional<UserActivityStats> findByUserId(UUID userId);

    void incrementPosts(UUID userId);

    void incrementComments(UUID userId);

    void incrementLikes(UUID userId, int delta);

    int incrementAllMemberDays();

    java.util.List<UUID> findUserIdsWithMemberDays(int days);

}
