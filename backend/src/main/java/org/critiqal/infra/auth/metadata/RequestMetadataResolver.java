package org.critiqal.infra.auth.metadata;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.HexFormat;

@RequestScoped
public class RequestMetadataResolver {

    private static final String HEADER_CF_IP = "CF-Connecting-IP";
    private static final String HEADER_CF_COUNTRY = "CF-IPCountry";
    private static final String HEADER_CF_CITY = "CF-IPCity";
    private static final String HEADER_XFF = "X-Forwarded-For";
    private static final String HEADER_DEVICE_ID = "X-Device-Id";
    private static final int MAX_UA_LENGTH = 512;
    private static final int MAX_CITY_LENGTH = 128;
    private static final HexFormat HEX = HexFormat.of();

    private final RoutingContext routingContext;

    public RequestMetadataResolver(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    public RequestMetadata resolve() {
        var req = routingContext.request();
        var rawIp = resolveIp(req);
        var countryCode = sanitizeCountry(req.getHeader(HEADER_CF_COUNTRY));
        var userAgent = truncate(req.getHeader("User-Agent"), MAX_UA_LENGTH);
        var rawDeviceId = req.getHeader(HEADER_DEVICE_ID);

        return new RequestMetadata(
                hash(rawIp),
                countryCode,
                truncate(req.getHeader(HEADER_CF_CITY), MAX_CITY_LENGTH),
                userAgent,
                detectPlatform(userAgent),
                hash(rawDeviceId)
        );
    }


    private String resolveIp(io.vertx.core.http.HttpServerRequest req) {
        var cfIp = req.getHeader(HEADER_CF_IP);
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }

        var xff = req.getHeader(HEADER_XFF);
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        var remote = req.remoteAddress();
        return remote != null ? remote.host() : null;
    }

    private String detectPlatform(String userAgent) {
        if (userAgent == null) return "unknown";
        var ua = userAgent.toLowerCase();
        if (ua.contains("telegram")) return "telegram";
        if (ua.contains("android")) return "android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "ios";
        if (ua.contains("windows")) return "windows";
        if (ua.contains("macintosh") || ua.contains("mac os x")) return "macos";
        if (ua.contains("linux")) return "linux";
        return "other";
    }

    private String sanitizeCountry(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("XX")) return null;
        var trimmed = raw.trim().toUpperCase();
        return trimmed.length() >= 2 ? trimmed.substring(0, 2) : null;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }

    public static String hash(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash failed", e);
        }
    }
}
