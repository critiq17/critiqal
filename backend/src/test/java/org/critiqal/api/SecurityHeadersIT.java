package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class SecurityHeadersIT {

    @Test
    void anyResponse_containsRequiredSecurityHeaders() {
        given()
                .when().get("/api/auth/me")
                .then()
                .header("X-Content-Type-Options", notNullValue())
                .header("Referrer-Policy", notNullValue())
                .header("Permissions-Policy", notNullValue())
                .header("X-Frame-Options", notNullValue());
    }
}
