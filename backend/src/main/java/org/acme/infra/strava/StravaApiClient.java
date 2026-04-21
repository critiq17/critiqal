package org.acme.infra.strava;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/*
    StravaApiClient
    Get user activities info
 */

@ApplicationScoped
public class StravaApiClient {

    // Strava API url
    private static final String BASE_URL = "https://www.strava.com/api/v3";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // Recently user activities
    public List<StravaActivity> getAthleteActivities(String accessToken, int perPage) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/athlete/activities?per_page=" + perPage))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new StravaException("Strava API error: " + response.statusCode());
        }

        // iteration of all recently activities
        JsonNode array = mapper.readTree(response.body());
        var activities = new ArrayList<StravaActivity>();
        for (JsonNode node : array) {
            activities.add(parseActivity(node));
        }
        return activities;
        } catch (StravaException e) {
            throw e;
        } catch (Exception e) {
            throw new StravaException("Failed to fetch activities", e);
        }
    }

    // mapped Json to StravaActivity record
    private StravaActivity parseActivity(JsonNode node) {
        return new StravaActivity(
                node.get("id").asLong(),
                node.path("name").asText(null),
                node.path("type").asText("Run"),
                node.path("distance").asDouble(),
                node.path("moving_time").asInt(),
                node.path("total_elevation_gain").asDouble(),
                Instant.parse(node.path("start_date").asText()),
                node.path("average_heartrate").asDouble(0),
                node.path("average_speed").asDouble()
        );
    }
}