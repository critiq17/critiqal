package org.critiqal.infra.telegram;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import org.critiqal.domain.alert.ErrorEvent;
import org.critiqal.domain.post.PostCreatedEvent;
import org.critiqal.domain.user.UserRegisteredEvent;

@ApplicationScoped
public class AlertObserver {

    private final AlertService alertService;

    public AlertObserver(AlertService alertService) {
        this.alertService = alertService;
    }

    void onUserRegistered(@ObservesAsync UserRegisteredEvent event) {
        alertService.info(
                "New user registered",
                "@" + event.username() + " (id=" + event.userId() + ")"
        );
    }

    void onPostCreated(@ObservesAsync PostCreatedEvent event) {
        alertService.info(
                "New post",
                "postId=" + event.postId() + " from userId=" + event.authorId()
        );
    }
    void onError(@ObservesAsync ErrorEvent event) {
        alertService.error(event.title(), event.details());
    }
}
