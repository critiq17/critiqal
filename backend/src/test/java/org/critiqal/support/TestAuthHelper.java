package org.critiqal.support;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.critiqal.domain.user.repository.UserRepository;
import org.critiqal.domain.user.Username;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public final class TestAuthHelper {

    public static final String COOKIE = "session";

    // Distinct IPs per registration so the per-IP rate limiter (5/hour) does
    // not collide across tests sharing the loopback bucket.
    private static final AtomicInteger IP_COUNTER = new AtomicInteger(0);

    private TestAuthHelper() {}

    public static Response registerAndGetResponse(String username) {
        var email = username + "@test.local";
        var response = baseRequest()
                .contentType(JSON)
                .body("""
                    {"username":"%s","password":"pass1234","email":"%s"}
                    """.formatted(username, email))
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().response();

        // New users start with emailVerified=false; @RequireVerifiedEmail
        // would block every subsequent write. Flip the flag directly so test
        // surface area matches the legacy contract.
        markEmailVerified(username);

        return response;
    }

    public static String registerAndGetSessionCookie(String username) {
        return registerAndGetResponse(username).getCookie(COOKIE);
    }

    public static UUID registerAndGetUserId(String username) {
        return UUID.fromString(registerAndGetResponse(username).jsonPath().getString("id"));
    }

    /** Used by tests that exercise the unverified-email path explicitly. */
    public static Response registerWithoutVerifying(String username) {
        var email = username + "@test.local";
        return baseRequest()
                .contentType(JSON)
                .body("""
                    {"username":"%s","password":"pass1234","email":"%s"}
                    """.formatted(username, email))
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().response();
    }

    private static RequestSpecification baseRequest() {
        // Synthetic per-call IPv4 keeps each registration in its own rate-limit
        // bucket. 198.51.100.0/24 is reserved for documentation/test ranges.
        var n = IP_COUNTER.incrementAndGet();
        var ip = "198.51.100." + (n % 250 + 1);
        return given()
                .header("X-Forwarded-For", ip)
                .header("X-Device-Id", "test-device-" + n);
    }

    private static void markEmailVerified(String username) {
        var repo = Arc.container().instance(UserRepository.class).get();
        QuarkusTransaction.requiringNew().run(() -> {
            repo.findByUsername(Username.of(username)).ifPresent(user -> {
                user.emailVerified = true;
                user.email = username + "@test.local";
                user.pendingEmail = null;
            });
        });
    }
}
