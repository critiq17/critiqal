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
    void register_duplicateUsername_returns400() {
        var body = "{\"username\":\"duplicate_user\",\"password\":\"pass123\"}";

        given().contentType(JSON).body(body)
                .when().post("/api/auth/register")
                .then().statusCode(201);
        given().contentType(JSON).body(body)
                .when().post("/api/auth/register")
                .then().statusCode(400);
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
}
