package org.critiqal.api.admin;

import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.api.admin.response.AdminUserDTO;
import org.critiqal.domain.badge.service.BadgeService;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.repository.UserRepository;

import java.util.List;
import java.util.UUID;

// Assembles admin user rows (identity + badges) with paging.
// Blank query browses recent users so the admin can explore with an empty search box.
@ApplicationScoped
public class AdminUserQueryService {

    private final UserRepository userRepo;
    private final BadgeService badgeService;

    public AdminUserQueryService(UserRepository userRepo, BadgeService badgeService) {
        this.userRepo = userRepo;
        this.badgeService = badgeService;
    }

    public Page<AdminUserDTO> search(String query, int page, int size) {
        int offset = page * size;
        boolean browse = query == null || query.isBlank();

        List<User> users = browse
                ? userRepo.findRecent(offset, size)
                : userRepo.searchPaged(query, offset, size);
        long total = browse
                ? userRepo.countAll()
                : userRepo.countSearch(query);

        List<AdminUserDTO> rows = users.stream()
                .map(u -> AdminUserDTO.from(u, badgeService.getUserBadges(u.id)))
                .toList();
        return Page.of(rows, page, size, total);
    }

    public AdminUserDTO getUser(UUID userId) {
        var user = userRepo.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return AdminUserDTO.from(user, badgeService.getUserBadges(user.id));
    }
}
