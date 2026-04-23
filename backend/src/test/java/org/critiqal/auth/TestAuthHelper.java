package org.critiqal.auth;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

/*
    TestAuthHelper - tester for auth API
 */
public class TestAuthHelper {

    // Test registered and claim JWT token
    public static String registerAndGetToken(String username) {
        return given()
                .contentType(JSON)
                .body("""
                    {"username":"%s","password":"pass123"}
                    """.formatted(username))
                .when().post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract().path("token");
    }

    // Tested registered and give user id
    public static Long registerAndGetUserId(String username) {
        return given()
                .contentType(JSON)
                .body("""
                    {"username":"%s","password":"pass123"}
                    """.formatted(username))
                .when().post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("user.id");
    }
}
