package org.critiqal.api.user;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.support.TestAuthHelper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserControllerIT {

    @Test
    void getProfile_returns200() {
        TestAuthHelper.registerAndGetSessionCookie("profile_get_user");

        given()
        .when().get("/api/users/profile_get_user")
        .then()
            .statusCode(200)
            .body("username", equalTo("profile_get_user"));
    }

    @Test
    void updateProfile_noAuth_returns401() {
        given()
            .contentType(JSON)
            .body("{\"name\":\"Alice\",\"bio\":\"hello\"}")
        .when().put("/api/users/me")
        .then().statusCode(401);
    }

    @Test
    void updateProfile_valid_returns200() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("profile_update_user");

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
            .contentType(JSON)
            .body("{\"name\":\"Alice\",\"bio\":\"my bio\"}")
        .when().put("/api/users/me")
        .then()
            .statusCode(200)
            .body("name", equalTo("Alice"))
            .body("bio", equalTo("my bio"));
    }

    @Test
    void follow_noAuth_returns401() {
        given()
            .contentType(JSON)
        .when().post("/api/users/" + uuid(1) + "/follow")
        .then().statusCode(401);
    }

    @Test
    void follow_self_returns400() {
        var selfId = TestAuthHelper.registerAndGetUserId("follow_self_user");
        var sid = given()
            .contentType(JSON)
            .body("{\"username\":\"follow_self_user\",\"password\":\"pass123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200)
        .extract().cookie(TestAuthHelper.COOKIE);

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
        .when().post("/api/users/" + selfId + "/follow")
        .then().statusCode(400);
    }

    @Test
    void follow_valid_returns204() {
        var followerSid = TestAuthHelper.registerAndGetSessionCookie("follow_follower_user");
        var targetId = TestAuthHelper.registerAndGetUserId("follow_target_user");

        given()
            .cookie(TestAuthHelper.COOKIE, followerSid)
        .when().post("/api/users/" + targetId + "/follow")
        .then().statusCode(200);
    }

    @Test
    void unfollow_valid_returns204() {
        var followerSid = TestAuthHelper.registerAndGetSessionCookie("unfollow_follower_user");
        var targetId = TestAuthHelper.registerAndGetUserId("unfollow_target_user");

        given()
            .cookie(TestAuthHelper.COOKIE, followerSid)
        .when().post("/api/users/" + targetId + "/follow")
        .then().statusCode(200);

        given()
            .cookie(TestAuthHelper.COOKIE, followerSid)
        .when().delete("/api/users/" + targetId + "/follow")
        .then().statusCode(204);
    }

    @Test
    void searchUsers_returns200() {
        TestAuthHelper.registerAndGetSessionCookie("search_target_user");

        given()
            .queryParam("q", "search_target")
        .when().get("/api/users/search")
        .then()
            .statusCode(200)
            .body("$", not(empty()));
    }

    private UUID uuid(int value) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(value));
    }
}
