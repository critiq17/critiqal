package org.critiqal.domain.event.service;

import org.critiqal.domain.badge.BadgeCode;
import org.critiqal.domain.badge.repository.UserBadgeRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventPermissionServiceTest {

    private final UserBadgeRepository userBadgeRepo = mock(UserBadgeRepository.class);
    private final EventPermissionService service = new EventPermissionService(userBadgeRepo);

    @Test
    void noOrganizerBadge_cannotCreate() {
        UUID user = UUID.randomUUID();
        lenient().when(userBadgeRepo.existsByUserIdAndBadgeCode(user, BadgeCode.AEDILE.name())).thenReturn(false);
        lenient().when(userBadgeRepo.existsByUserIdAndBadgeCode(user, BadgeCode.PRAETOR.name())).thenReturn(false);
        lenient().when(userBadgeRepo.existsByUserIdAndBadgeCode(user, BadgeCode.CONSUL.name())).thenReturn(false);

        assertThat(service.canCreateEvents(user)).isFalse();
    }

    @Test
    void aedile_canCreate() {
        UUID user = UUID.randomUUID();
        when(userBadgeRepo.existsByUserIdAndBadgeCode(user, BadgeCode.AEDILE.name())).thenReturn(true);

        assertThat(service.canCreateEvents(user)).isTrue();
    }

    @Test
    void consul_canCreate() {
        UUID user = UUID.randomUUID();
        lenient().when(userBadgeRepo.existsByUserIdAndBadgeCode(user, BadgeCode.AEDILE.name())).thenReturn(false);
        lenient().when(userBadgeRepo.existsByUserIdAndBadgeCode(user, BadgeCode.PRAETOR.name())).thenReturn(false);
        when(userBadgeRepo.existsByUserIdAndBadgeCode(user, BadgeCode.CONSUL.name())).thenReturn(true);

        assertThat(service.canCreateEvents(user)).isTrue();
    }

    @Test
    void nullUser_cannotCreate() {
        assertThat(service.canCreateEvents(null)).isFalse();
    }
}
