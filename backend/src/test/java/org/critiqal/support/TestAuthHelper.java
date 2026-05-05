package org.critiqal.support;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public final class TestAuthHelper {

    public static final String COOKIE = "session";

    private TestAuthHelper() {}

    public static Response registerAndGetResponse(String username) {
        return given()
                .contentType(JSON)
                .body("""
                    {"username":"%s","password":"pass123"}
                    """.formatted(username))
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().response();
    }

    public static String registerAndGetSessionCookie(String username) {
        return registerAndGetResponse(username).getCookie(COOKIE);
    }

    public static Long registerAndGetUserId(String username) {
        return registerAndGetResponse(username).jsonPath().getLong("id");
    }
}
