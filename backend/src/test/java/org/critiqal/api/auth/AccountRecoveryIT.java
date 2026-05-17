package org.critiqal.api.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.support.TestAuthHelper;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AccountRecoveryIT {

    @Test
    void requestReset_unknownEmail_returns200() {
        given().contentType(JSON).body("{\"email\":\"nobody@nowhere.com\"}")
                .when().post("/api/auth/recovery/request").then().statusCode(200);
    }

    @Test
    void resetPassword_fakeToken_returns400() {
        given().contentType(JSON)
                .body("{\"token\":\"fake\",\"newPassword\":\"newpass123\"}")
                .when().post("/api/auth/recovery/reset").then().statusCode(400);
    }

    @Test
    void regenerateCodes_requiresAuth() {
        given().when().post("/api/auth/recovery/codes/regenerate").then().statusCode(401);
    }

    @Test
    void regenerateCodes_returns8Codes() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("regen_it_user");
        given().cookie("session", sid)
                .when().post("/api/auth/recovery/codes/regenerate")
                .then().statusCode(200)
                .body("codes", hasSize(8))
                .body("warning", notNullValue());
    }

    @Test
    void setEmail_requiresAuth() {
        given().contentType(JSON).body("{\"email\":\"x@x.com\"}")
                .when().post("/api/auth/email").then().statusCode(401);
    }
}
