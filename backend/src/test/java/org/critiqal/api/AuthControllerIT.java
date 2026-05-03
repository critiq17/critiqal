package org.critiqal.api;

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
            .body("{\"username\":\"newuser_auth\",\"password\":\"pass123\"}")
        .when().post("/api/auth/register")
        .then()
            .statusCode(201)
            .cookie("session", notNullValue())
            .body("username", equalTo("newuser_auth"))
            .body("id", notNullValue());
    }

    @Test
    void register_duplicateUsername_returns409() {
        var body = "{\"username\":\"duplicate_user\",\"password\":\"pass123\"}";

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
                .body("{\"username\":\"login_valid_user\",\"password\":\"pass123\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201);

        given()
            .contentType(JSON)
            .body("{\"username\":\"login_valid_user\",\"password\":\"pass123\"}")
        .when().post("/api/auth/login")
        .then()
            .statusCode(200)
            .cookie("session", notNullValue())
            .body("username", equalTo("login_valid_user"));
    }

    @Test
    void login_wrongPassword_returns401() {
        given().contentType(JSON)
                .body("{\"username\":\"wrong_pass_user\",\"password\":\"pass123\"}")
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
                .body("{\"username\":\"me_user\",\"password\":\"pass123\"}")
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
            .body("{\"username\":\"ghost_user_404\",\"password\":\"pass123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(401);
    }

    @Test
    void revokeSession_ownSession_returns204() {
        // Register and capture the session ID from login response
        var loginResp = given().contentType(JSON)
                .body("{\"username\":\"revoke_user\",\"password\":\"pass123\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().response();

        var sid = loginResp.getCookie("session");

        // Revoke the own session
        given()
            .cookie("session", sid)
        .when().delete("/api/auth/sessions/" + sid)
        .then().statusCode(204);

        // Verify the session is gone — /me must return 401 now
        given()
            .cookie("session", sid)
        .when().get("/api/auth/me")
        .then().statusCode(401);
    }

    @Test
    void revokeSession_otherUserSession_returns403() {
        var aliceSid = given().contentType(JSON)
                .body("{\"username\":\"alice_revoke\",\"password\":\"pass123\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie("session");

        var bobSid = given().contentType(JSON)
                .body("{\"username\":\"bob_revoke\",\"password\":\"pass123\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie("session");

        // Bob tries to revoke Alice's session
        given()
            .cookie("session", bobSid)
        .when().delete("/api/auth/sessions/" + aliceSid)
        .then().statusCode(403);
    }

    @Test
    void revokeSession_unauthenticated_returns401() {
        given()
        .when().delete("/api/auth/sessions/some-random-id")
        .then().statusCode(401);
    }
}
