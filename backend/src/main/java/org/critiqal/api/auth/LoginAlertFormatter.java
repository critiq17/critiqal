package org.critiqal.api.auth;

import org.critiqal.infra.auth.metadata.CountryNames;
import org.critiqal.infra.auth.metadata.RequestMetadata;
import org.critiqal.infra.auth.metadata.UserAgentParser;

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
        var device = describeDevice(metadata);

        if (location != null && device != null) return location + " on " + device;
        if (location != null) return location;
        if (device != null) return "an unknown location on " + device;
        return "an unknown location";
    }

    private static String describeLocation(RequestMetadata metadata) {
        var parts = new ArrayList<String>(2);
        append(parts, metadata.city());
        var country = CountryNames.resolve(metadata.countryCode());
        append(parts, country != null ? country : metadata.countryCode());
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static String describeDevice(RequestMetadata metadata) {
        var browser = UserAgentParser.browser(metadata.userAgent());
        var platform = normalize(prettyPlatform(metadata.platform()));

        if (browser != null && platform != null) return browser + " on " + platform;
        if (browser != null) return browser;
        return platform;
    }

    private static String prettyPlatform(String platform) {
        if (platform == null) return null;
        return switch (platform.toLowerCase()) {
            case "ios" -> "iOS";
            case "macos" -> "macOS";
            case "android" -> "Android";
            case "windows" -> "Windows";
            case "linux" -> "Linux";
            case "telegram" -> "Telegram";
            case "unknown", "other" -> null;
            default -> platform;
        };
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
