package org.critiqal.api;

import io.quarkus.security.identity.SecurityIdentity;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserTest {

    @Test
    void id_returnsAuthenticatedUserId() {
        var identity = authenticatedIdentity("42");

        var currentUser = new CurrentUser(identity);

        assertThat(currentUser.id()).isEqualTo(42L);
    }

    @Test
    void idOrNull_returnsNullForAnonymousUser() {
        var identity = mock(SecurityIdentity.class);
        when(identity.isAnonymous()).thenReturn(true);

        var currentUser = new CurrentUser(identity);

        assertThat(currentUser.idOrNull()).isNull();
    }

    @Test
    void id_throwsForbiddenForAnonymousUser() {
        var identity = mock(SecurityIdentity.class);
        when(identity.isAnonymous()).thenReturn(true);

        var currentUser = new CurrentUser(identity);

        assertThrows(ForbiddenException.class, currentUser::id);
    }

    @Test
    void id_throwsIllegalArgumentExceptionForInvalidPrincipal() {
        var identity = authenticatedIdentity("not-a-number");

        var currentUser = new CurrentUser(identity);

        assertThrows(IllegalArgumentException.class, currentUser::id);
    }

    private SecurityIdentity authenticatedIdentity(String principalName) {
        var identity = mock(SecurityIdentity.class);
        when(identity.isAnonymous()).thenReturn(false);
        when(identity.getPrincipal()).thenReturn((Principal) () -> principalName);
        return identity;
    }
}
