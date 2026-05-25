package org.critiqal.infra.auth.metadata;

public record RequestMetadata(
        String ipHash,
        String countryCode,
        String city,
        String userAgent,
        String platform,
        String deviceIdHash
) {
    public static final RequestMetadata EMPTY =
            new RequestMetadata(null, null, null, null, null, null);
}
