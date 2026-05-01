package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.auth.TestAuthHelper;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;


@QuarkusTest
public class SessionAuthIT {

    @Test
    void login_setsCookie_meWorks_logoutClears_meReturns401() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("alice");
        assertThat(sid).isNotBlank();

        given().cookie("session", sid)
                .when().get("/api/auth/me")
                .then().statusCode(200)
                .body("username", equalTo("alice"));

        var afterLogout = given().cookie("session", sid)
                .when().post("/api/auth/logout")
                .then().statusCode(204)
                .extract();
        assertThat(afterLogout.cookie("session")).isEmpty();

        given().cookie("session", sid)
                .when().get("/api/auth/me")
                .then().statusCode(401);
    }
}
