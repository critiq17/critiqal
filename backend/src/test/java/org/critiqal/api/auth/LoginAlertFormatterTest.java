package org.critiqal.api.auth;

import org.critiqal.infra.auth.metadata.RequestMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAlertFormatterTest {

    @Test
    void buildMessage_includesCityFullCountryAndPlatform_whenAvailable() {
        var metadata = new RequestMetadata(null, "CZ", "Prague",
                "Mozilla/5.0 (X11; Linux x86_64) Firefox/130", "linux", null);

        assertThat(LoginAlertFormatter.buildMessage(metadata))
                .contains("from Prague, Czechia on Firefox on Linux.");
    }

    @Test
    void buildMessage_fallsBackToCountryCode_whenIsoNotResolved() {
        var metadata = new RequestMetadata(null, "ZZ", null, null, "windows", null);

        assertThat(LoginAlertFormatter.buildMessage(metadata))
                .contains("from ZZ on Windows.");
    }

    @Test
    void buildMessage_usesUnknownLocation_whenGeoHeadersMissing() {
        var metadata = new RequestMetadata(null, null, null,
                "Mozilla/5.0 (X11; Linux x86_64) Firefox/130", "linux", null);

        assertThat(LoginAlertFormatter.buildMessage(metadata))
                .contains("from an unknown location on Firefox on Linux.");
    }

    @Test
    void buildMessage_usesGenericUnknownLocation_whenEverythingMissing() {
        assertThat(LoginAlertFormatter.buildMessage(RequestMetadata.EMPTY))
                .contains("from an unknown location.");
    }
}
