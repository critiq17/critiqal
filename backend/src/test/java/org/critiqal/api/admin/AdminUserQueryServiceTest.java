package org.critiqal.api.admin;

import org.critiqal.api.badge.response.UserBadgeDTO;
import org.critiqal.domain.badge.BadgeCode;
import org.critiqal.domain.badge.service.BadgeService;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Plain unit test (no Quarkus context needed): the service only depends on
// two interfaces, which we mock. Mirrors BadgeServiceImplTest's mock(...) style.
class AdminUserQueryServiceTest {

    UserRepository userRepo;
    BadgeService badgeService;
    AdminUserQueryService service;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        badgeService = mock(BadgeService.class);
        service = new AdminUserQueryService(userRepo, badgeService);
    }

    private static User user(String username) {
        var u = new User();
        u.id = UUID.randomUUID();
        u.username = username;
        u.name = username + " name";
        u.createdAt = Instant.now();
        return u;
    }

    @Test
    @DisplayName("blank query browses recent users (not search)")
    void search_blankQuery_browsesRecent() {
        var u = user("alice");
        when(userRepo.findRecent(0, 20)).thenReturn(List.of(u));
        when(userRepo.countAll()).thenReturn(1L);
        when(badgeService.getUserBadges(u.id)).thenReturn(List.of());

        Page<?> page = service.search("  ", 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.total()).isEqualTo(1L);
        verify(userRepo).findRecent(0, 20);
        verify(userRepo, never()).searchPaged(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("non-blank query uses paged search with correct offset and includes badges")
    void search_query_usesSearchPaged() {
        var u = user("bob");
        when(userRepo.searchPaged("bob", 40, 20)).thenReturn(List.of(u));
        when(userRepo.countSearch("bob")).thenReturn(5L);
        when(badgeService.getUserBadges(u.id)).thenReturn(List.of(
                new UserBadgeDTO(UUID.randomUUID(), BadgeCode.ORIGIN, "Origin", "desc", null, null, Instant.now())
        ));

        var page = service.search("bob", 2, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).username()).isEqualTo("bob");
        assertThat(page.content().get(0).badges()).hasSize(1);
        assertThat(page.page()).isEqualTo(2);
        verify(userRepo).searchPaged("bob", 40, 20);
    }

    @Test
    @DisplayName("getUser throws NotFound for missing id")
    void getUser_missing_throws() {
        var id = UUID.randomUUID();
        when(userRepo.findByIdOptional(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getUser returns a row with the user's badges")
    void getUser_present_returnsRow() {
        var u = user("carol");
        when(userRepo.findByIdOptional(u.id)).thenReturn(Optional.of(u));
        when(badgeService.getUserBadges(u.id)).thenReturn(List.of());

        var row = service.getUser(u.id);

        assertThat(row.id()).isEqualTo(u.id);
        assertThat(row.username()).isEqualTo("carol");
    }
}
