package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

@QuarkusTest
public class SecurityIT {
    @ParameterizedTest
    @CsvSource({
            "POST, /api/posts",
            "DELETE, /api/posts/1",
            "POST, /api/posts/1/comments",
            "PUT, /api/users/me",
            "POST, /api/users/1/follow",
            "DELETE, /api/users/1/follow",
            "GET, /api/auth/me",
            // "POST, /api/media/avatar",
            "GET, /api/integrations/strava/connect",
            "DELETE, /api/integrations/strava",
    })
    void protectedEndpoint_noToken_returns401(String method, String path) {
        given()
                .contentType(JSON)
                .request(method, path)
                .then()
                .statusCode(401);
    }
}
