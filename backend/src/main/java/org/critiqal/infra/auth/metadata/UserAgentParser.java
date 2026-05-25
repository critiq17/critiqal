package org.critiqal.infra.auth.metadata;

/**
 * Lightweight user-agent inspection. Returns a short, human-friendly
 * browser label without pulling in a full UA-parsing dependency.
 */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    public static String browser(String userAgent) {
        if (userAgent == null) return null;
        var ua = userAgent.toLowerCase();
        if (ua.contains("telegram")) return "Telegram";
        if (ua.contains("edg/") || ua.contains("edge/")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("crios")) return "Chrome";
        if (ua.contains("fxios")) return "Firefox";
        if (ua.contains("chrome") && !ua.contains("chromium")) return "Chrome";
        if (ua.contains("chromium")) return "Chromium";
        if (ua.contains("safari")) return "Safari";
        return null;
    }
}
