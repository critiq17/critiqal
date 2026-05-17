package org.critiqal.domain.auth.email;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.user.repository.UserRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PendingEmailCleanupJob {

    private static final Logger log = Logger.getLogger(PendingEmailCleanupJob.class);

    private final UserRepository userRepo;

    public PendingEmailCleanupJob(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Scheduled(every = "1h")
    @Transactional
    public void clearExpiredPendingEmails() {
        int cleared = userRepo.clearExpiredPendingEmails();
        if (cleared > 0) {
            log.infof("Cleared %d expired pending email(s)", cleared);
        }
    }
}
