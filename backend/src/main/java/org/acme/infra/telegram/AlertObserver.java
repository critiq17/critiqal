package org.acme.infra.telegram;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import org.acme.domain.alert.ErrorEvent;
import org.acme.domain.post.PostCreatedEvent;
import org.acme.domain.user.UserRegisteredEvent;

@ApplicationScoped
public class AlertObserver {

    @Inject AlertService alertService;

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
