package org.critiqal.api.auth;

import org.critiqal.infra.auth.metadata.RequestMetadata;

import java.util.ArrayList;
import java.util.List;

final class LoginAlertFormatter {

    private LoginAlertFormatter() {
    }

    static String buildMessage(RequestMetadata metadata) {
        return "A new login was detected on your Critiqal account from " +
                describeContext(metadata) +
                ". If this was you, no action is needed. If not, revoke this " +
                "session immediately in Settings -> Sessions.";
    }

    static String describeContext(RequestMetadata metadata) {
        var location = describeLocation(metadata);
        var platform = normalize(metadata.platform());

        if (location != null && platform != null && !"unknown".equals(platform)) {
            return location + " on " + platform;
        }
        if (location != null) {
            return location;
        }
        if (platform != null && !"unknown".equals(platform)) {
            return "an unknown location on " + platform;
        }
        return "an unknown location";
    }

    private static String describeLocation(RequestMetadata metadata) {
        var parts = new ArrayList<String>(2);
        append(parts, metadata.city());
        append(parts, metadata.countryCode());
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static void append(List<String> parts, String value) {
        var normalized = normalize(value);
        if (normalized != null) {
            parts.add(normalized);
        }
    }

    private static String normalize(String value) {
        if (value == null) return null;
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
