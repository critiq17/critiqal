package org.critiqal.api.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.support.TestAuthHelper;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class TwoFactorIT {

    @Test
    void setup_requiresAuth() {
        given().when().post("/api/auth/2fa/setup").then().statusCode(401);
    }

    @Test
    void setup_returnsQrAndRecoveryCodes() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("totp_it_user");
        given().cookie("session", sid).when().post("/api/auth/2fa/setup")
                .then().statusCode(200)
                .body("qrCodeUri", notNullValue())
                .body("secret", notNullValue())
                .body("recoveryCodes", hasSize(8));
    }

    @Test
    void confirm_badCode_returns400() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("totp_confirm_it_user");
        given().cookie("session", sid).when().post("/api/auth/2fa/setup").then().statusCode(200);
        given().cookie("session", sid).contentType(JSON).body("{\"code\":\"000000\"}")
                .when().post("/api/auth/2fa/confirm").then().statusCode(400);
    }

    @Test
    void status_default_isFalse() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("totp_status_it_user");
        given().cookie("session", sid).when().get("/api/auth/2fa/status")
                .then().statusCode(200).body("enabled", equalTo(false));
    }

    @Test
    void loginWith2fa_invalidChallenge_returns401() {
        given().contentType(JSON)
                .body("{\"challengeToken\":\"fake-token\",\"code\":\"123456\"}")
                .when().post("/api/auth/login/2fa").then().statusCode(401);
    }
}
