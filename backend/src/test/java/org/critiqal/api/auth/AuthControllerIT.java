package org.critiqal.api.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthControllerIT {

    @Test
    void register_validData_returns201WithCookieAndUser() {
        given()
            .contentType(JSON)
            .body("{\"username\":\"newuser_auth\",\"password\":\"pass1234\",\"email\":\"newuser_auth@test.local\"}")
        .when().post("/api/auth/register")
        .then()
            .statusCode(201)
            .cookie("session", notNullValue())
            .body("username", equalTo("newuser_auth"))
            .body("id", notNullValue());
    }

    @Test
    void register_duplicateUsername_returns409() {
        var body = "{\"username\":\"duplicate_user\",\"password\":\"pass1234\",\"email\":\"duplicate_user@test.local\"}";

        given().contentType(JSON).body(body)
                .when().post("/api/auth/register")
                .then().statusCode(201);
        given().contentType(JSON).body(body)
                .when().post("/api/auth/register")
                .then().statusCode(409);
    }

    @Test
    void login_validCredentials_returnsCookie() {
        given().contentType(JSON)
                .body("{\"username\":\"login_valid_user\",\"password\":\"pass1234\",\"email\":\"login_valid_user@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201);

        given()
            .contentType(JSON)
            .body("{\"username\":\"login_valid_user\",\"password\":\"pass1234\"}")
        .when().post("/api/auth/login")
        .then()
            .statusCode(200)
            .cookie("session", notNullValue())
            .body("username", equalTo("login_valid_user"));
    }

    @Test
    void login_wrongPassword_returns401() {
        given().contentType(JSON)
                .body("{\"username\":\"wrong_pass_user\",\"password\":\"pass1234\",\"email\":\"wrong_pass_user@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201);

        given()
            .contentType(JSON)
            .body("{\"username\":\"wrong_pass_user\",\"password\":\"wrong\"}")
        .when().post("/api/auth/login")
        .then().statusCode(401);
    }

    @Test
    void me_noCookie_returns401() {
        given()
                .when().get("/api/auth/me")
                .then().statusCode(401);
    }

    @Test
    void me_validCookie_returnsUser() {
        var sid = given().contentType(JSON)
                .body("{\"username\":\"me_user\",\"password\":\"pass1234\",\"email\":\"me_user@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie("session");

        given()
                .cookie("session", sid)
                .when().get("/api/auth/me")
                .then()
                .statusCode(200)
                .body("username", equalTo("me_user"));
    }

    @Test
    void login_unknownUser_returns401() {
        given()
            .contentType(JSON)
            .body("{\"username\":\"ghost_user_404\",\"password\":\"pass1234\"}")
        .when().post("/api/auth/login")
        .then().statusCode(401);
    }

    @Test
    void revokeSession_ownSession_returns204() {
        var sid = given().contentType(JSON)
                .body("{\"username\":\"revoke_user\",\"password\":\"pass1234\",\"email\":\"revoke_user@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie("session");

        var sessionId = given()
                .cookie("session", sid)
                .when().get("/api/auth/sessions")
                .then().statusCode(200)
                .extract().jsonPath().getString("[0].id");

        given()
            .cookie("session", sid)
        .when().delete("/api/auth/sessions/" + sessionId)
        .then().statusCode(204);

        given()
            .cookie("session", sid)
        .when().get("/api/auth/me")
        .then().statusCode(401);
    }

    @Test
    void revokeSession_otherUserSession_returns403() {
        var aliceSid = given().contentType(JSON)
                .body("{\"username\":\"alice_revoke\",\"password\":\"pass1234\",\"email\":\"alice_revoke@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie("session");

        var bobSid = given().contentType(JSON)
                .body("{\"username\":\"bob_revoke\",\"password\":\"pass1234\",\"email\":\"bob_revoke@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie("session");

        var aliceSessionId = given()
                .cookie("session", aliceSid)
                .when().get("/api/auth/sessions")
                .then().statusCode(200)
                .extract().jsonPath().getString("[0].id");

        given()
            .cookie("session", bobSid)
        .when().delete("/api/auth/sessions/" + aliceSessionId)
        .then().statusCode(403);
    }

    @Test
    void revokeSession_unauthenticated_returns401() {
        given()
        .when().delete("/api/auth/sessions/some-random-id")
        .then().statusCode(401);
    }

    @Test
    void register_shortPassword_returns400() {
        given()
            .contentType(JSON)
            .body("{\"username\":\"short_pw_user\",\"password\":\"123\",\"email\":\"short_pw_user@test.local\"}")
        .when().post("/api/auth/register")
        .then().statusCode(400);
    }
}
