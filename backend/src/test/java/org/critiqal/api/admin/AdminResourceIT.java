package org.critiqal.api.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;

// Verifies the admin authorization boundary end-to-end against a booted app.
// AdminSecurityExceptionMapper hides every /api/admin endpoint behind a 404 for
// callers without a valid admin session, so unauthenticated access must never
// reveal 401/403. Runs under failsafe (Dev Services provide Postgres + Redis).
@QuarkusTest
class AdminResourceIT {

    @Test
    @DisplayName("GET /api/admin/me without admin session returns 404")
    void me_noSession_404() {
        given().when().get("/api/admin/me").then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/admin/users/search without admin session returns 404")
    void usersSearch_noSession_404() {
        given().when().get("/api/admin/users/search?q=a").then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} without admin session returns 404")
    void getUser_noSession_404() {
        given().when().get("/api/admin/users/" + UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/admin/posts/search without admin session returns 404")
    void postsSearch_noSession_404() {
        given().when().get("/api/admin/posts/search?q=a").then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/admin/badges without admin session returns 404")
    void badges_noSession_404() {
        given().when().get("/api/admin/badges").then().statusCode(404);
    }

    @Test
    @DisplayName("POST grant badge without admin session returns 404")
    void grant_noSession_404() {
        given().contentType("application/json").body("{\"code\":\"ORIGIN\"}")
                .when().post("/api/admin/users/" + UUID.randomUUID() + "/badges")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE revoke badge without admin session returns 404")
    void revoke_noSession_404() {
        given().when().delete("/api/admin/users/" + UUID.randomUUID() + "/badges/ORIGIN")
                .then().statusCode(404);
    }
}
