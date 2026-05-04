package org.critiqal.infra.strava;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.shared.exception.strava.StravaException;
import org.critiqal.domain.strava.gateway.Strava0AuthClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/*
    StravaOAuthClient - strava OAuth client
    Implement methods for authorize users
 */

@ApplicationScoped
public class StravaOAuthClientImpl implements Strava0AuthClient {

    // URLs which make tokens
    private static final String TOKEN_URL = "https://www.strava.com/oauth/token";
    private static final String AUTH_URL = "https://www.strava.com/oauth/authorize";

    // private keys for OAuth app
    @ConfigProperty(name = "strava.client-id") String clientId;
    @ConfigProperty(name = "strava.client-secret") String clientSecret;
    @ConfigProperty(name = "strava.redirect-uri") String redirectUri;

    // HttpClient which send requests to Strava API
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ObjectMapper - mapper (json <-> java types)
    private final ObjectMapper mapper = new ObjectMapper();

    // create authorization link by clientId and clientSecret
    public String buildAuthorizationUrl(String state) {
        return AUTH_URL
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&approval_prompt=auto"
                + "&scope=read,activity:read"
                + "&state=" + state;
    }

    public StravaTokenResponse exchangeCode(String code) {
        return postTokenRequest(
                "grant_type=authorization_code"
                + "&code=" + code
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret
        );
    }

    // refreshed auth token
    public StravaTokenResponse refreshToken(String tokenRefresh) {
        return postTokenRequest(
                "grant_type=refresh_token"
                + "&refresh_token=" + tokenRefresh
                + "&client_id=" + clientId
                + "&client_secret" + clientSecret
        );
    }

    // received token
    private StravaTokenResponse postTokenRequest(String body) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new StravaException("Strava OAuth failed: " + response.statusCode());
            }

            JsonNode json = mapper.readTree(response.body());
            return new StravaTokenResponse(
                    json.get("access_token").asText(),
                    json.get("refresh_token").asText(),
                    json.get("expires_at").asLong(),
                    parseAthlete(json.get("athlete"))
            );
        } catch (StravaException e) {
            throw e;
        } catch (Exception e) {
            throw new StravaException("Strava OAuth request failed", e);
        }
    }

    // mapped json data to java
    private StravaAthleteInfo parseAthlete(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return new StravaAthleteInfo(
                node.get("id").asLong(),
                node.path("username").asText(null),
                node.path("firstname").asText(null),
                node.path("lastname").asText(null),
                node.path("city").asText(null),
                node.path("profile_medium").asText(null)
        );
    }
}
