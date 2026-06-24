package org.critiqal.api.auth;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.critiqal.support.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AccountRecoveryIT {

    @Inject MockMailbox mailbox;

    @BeforeEach
    void clearMailbox() {
        mailbox.clear();
    }

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

    @Test
    void resetPassword_invalidatesExistingSession() {
        var username = "reset_session_it_user";
        var email = username + "@test.local";
        var session = TestAuthHelper.registerAndGetSessionCookie(username);

        // old session works before reset
        given().cookie("session", session)
                .when().get("/api/users/me").then().statusCode(200);

        given().contentType(JSON).body("{\"email\":\"%s\"}".formatted(email))
                .when().post("/api/auth/recovery/request").then().statusCode(200);

        var messages = mailbox.getMessagesSentTo(email);
        assertThat(messages).isNotEmpty();
        var body = messages.get(0).getHtml();
        var matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
        assertThat(matcher.find()).isTrue();
        var rawToken = matcher.group(1);

        given().contentType(JSON)
                .body("{\"token\":\"%s\",\"newPassword\":\"newSecurePass99\"}".formatted(rawToken))
                .when().post("/api/auth/recovery/reset").then().statusCode(200);

        // old session must be rejected after password reset
        given().cookie("session", session)
                .when().get("/api/users/me").then().statusCode(401);
    }
}
