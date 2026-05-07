package org.critiqal.infra.auth.session;

import jakarta.ws.rs.core.NewCookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionFactoryCookieTest {

    @Test
    void issueAndExpire_useConfiguredSameSite() {
        var factory = new SessionFactoryCookie("session", true, "None", 30);

        var issued = factory.issue("abc");
        var expired = factory.expire();

        assertThat(issued.getSameSite()).isEqualTo(NewCookie.SameSite.NONE);
        assertThat(issued.isSecure()).isTrue();
        assertThat(issued.isHttpOnly()).isTrue();
        assertThat(expired.getSameSite()).isEqualTo(NewCookie.SameSite.NONE);
        assertThat(expired.isSecure()).isTrue();
        assertThat(expired.isHttpOnly()).isTrue();
        assertThat(expired.getMaxAge()).isZero();
    }

    @Test
    void constructor_trimsCookieName() {
        var factory = new SessionFactoryCookie("  session  ", false, "Lax", 30);

        var issued = factory.issue("abc");

        assertThat(factory.name()).isEqualTo("session");
        assertThat(issued.getName()).isEqualTo("session");
    }

    @Test
    void constructor_rejectsSameSiteNoneWithoutSecure() {
        assertThrows(IllegalArgumentException.class,
                () -> new SessionFactoryCookie("session", false, "None", 30));
    }
}
