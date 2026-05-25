package org.critiqal.api.auth;

import org.critiqal.infra.auth.metadata.RequestMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAlertFormatterTest {

    @Test
    void buildMessage_includesCityCountryAndPlatform_whenAvailable() {
        var metadata = new RequestMetadata(null, "CZ", "Prague", null, "linux", null);

        assertThat(LoginAlertFormatter.buildMessage(metadata))
                .contains("from Prague, CZ on linux.");
    }

    @Test
    void buildMessage_usesUnknownLocation_whenGeoHeadersMissing() {
        var metadata = new RequestMetadata(null, null, null, null, "linux", null);

        assertThat(LoginAlertFormatter.buildMessage(metadata))
                .contains("from an unknown location on linux.");
    }

    @Test
    void buildMessage_usesGenericUnknownLocation_whenEverythingMissing() {
        assertThat(LoginAlertFormatter.buildMessage(RequestMetadata.EMPTY))
                .contains("from an unknown location.");
    }
}
