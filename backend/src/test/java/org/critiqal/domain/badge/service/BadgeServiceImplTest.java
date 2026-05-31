package org.critiqal.domain.badge.service;

import org.critiqal.domain.badge.Badge;
import org.critiqal.domain.badge.BadgeCode;
import org.critiqal.domain.badge.UserBadge;
import org.critiqal.domain.badge.repository.BadgeRepository;
import org.critiqal.domain.badge.repository.UserBadgeRepository;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BadgeServiceImplTest {

    private final BadgeRepository badgeRepo = mock(BadgeRepository.class);
    private final UserBadgeRepository userBadgeRepo = mock(UserBadgeRepository.class);
    private final UserService userService = mock(UserService.class);

    private BadgeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BadgeServiceImpl(badgeRepo, userBadgeRepo, userService);
    }

    @Test
    void awardBadge_idempotent_doesNotSaveTwice() {
        var userId = uuid(1);
        when(userBadgeRepo.existsByUserIdAndBadgeCode(userId, BadgeCode.ORIGIN.name())).thenReturn(true);

        service.awardBadge(userId, BadgeCode.ORIGIN, Map.of());

        verify(userBadgeRepo, never()).save(any());
    }

    @Test
    void awardBadge_newBadge_savesUserBadge() {
        var userId = uuid(2);
        var user = new User(); user.id = userId;
        var badge = new Badge(); badge.code = BadgeCode.ORIGIN.name(); badge.name = "Origin";

        when(userBadgeRepo.existsByUserIdAndBadgeCode(userId, BadgeCode.ORIGIN.name())).thenReturn(false);
        when(userService.getById(userId)).thenReturn(user);
        when(badgeRepo.findByCode(BadgeCode.ORIGIN)).thenReturn(Optional.of(badge));
        when(userBadgeRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.awardBadge(userId, BadgeCode.ORIGIN, Map.of());

        var cap = org.mockito.ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepo).save(cap.capture());
        assertSame(user, cap.getValue().user);
        assertSame(badge, cap.getValue().badge);
    }

    @Test
    void awardBadge_badgeNotFound_throwsNotFoundException() {
        var userId = uuid(3);
        var user = new User(); user.id = userId;

        when(userBadgeRepo.existsByUserIdAndBadgeCode(userId, "GLADIATOR")).thenReturn(false);
        when(userService.getById(userId)).thenReturn(user);
        when(badgeRepo.findByCode(BadgeCode.GLADIATOR)).thenReturn(Optional.empty());

        assertThrows(org.critiqal.domain.shared.exception.NotFoundException.class,
                () -> service.awardBadge(userId, BadgeCode.GLADIATOR, Map.of()));

        verify(userBadgeRepo, never()).save(any());
    }

    @Test
    void getUserBadges_returnsMappedDTOs() {
        var userId = uuid(4);
        var badge = new Badge(); badge.id = uuid(10); badge.code = "SCRIBE"; badge.name = "Scribe";
        var ub = new UserBadge(); ub.badge = badge; ub.metadata = Map.of();

        when(userBadgeRepo.findByUserId(userId)).thenReturn(List.of(ub));

        var result = service.getUserBadges(userId);

        assertEquals(1, result.size());
        assertEquals(BadgeCode.SCRIBE, result.getFirst().code());
    }

    @Test
    void getUserBadges_noUserBadges_returnsEmptyList() {
        var userId = uuid(5);
        when(userBadgeRepo.findByUserId(userId)).thenReturn(List.of());

        var result = service.getUserBadges(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void countByCode_delegatesToRepository() {
        when(userBadgeRepo.countByBadgeCode("GLADIATOR")).thenReturn(42L);

        assertEquals(42L, service.countByCode(BadgeCode.GLADIATOR));
    }

    private static UUID uuid(int v) { return new UUID(0, v); }
}
