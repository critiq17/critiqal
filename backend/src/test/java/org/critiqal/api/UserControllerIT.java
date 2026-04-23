package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.auth.TestAuthHelper;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserControllerIT {

    @Test
    void getProfile_returns200() {
        TestAuthHelper.registerAndGetToken("profile_get_user");

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
        var token = TestAuthHelper.registerAndGetToken("profile_update_user");

        given()
            .header("Authorization", "Bearer " + token)
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
        .when().post("/api/users/1/follow")
        .then().statusCode(401);
    }

    @Test
    void follow_self_returns400() {
        var selfId = TestAuthHelper.registerAndGetUserId("follow_self_user");
        var token = given()
            .contentType(JSON)
            .body("{\"username\":\"follow_self_user\",\"password\":\"pass123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200)
        .extract().path("token");

        given()
            .header("Authorization", "Bearer " + token)
        .when().post("/api/users/" + selfId + "/follow")
        .then().statusCode(400);
    }

    @Test
    void follow_valid_returns204() {
        var followerToken = TestAuthHelper.registerAndGetToken("follow_follower_user");
        var targetId = TestAuthHelper.registerAndGetUserId("follow_target_user");

        given()
            .header("Authorization", "Bearer " + followerToken)
        .when().post("/api/users/" + targetId + "/follow")
        .then().statusCode(200);
    }

    @Test
    void unfollow_valid_returns204() {
        var followerToken = TestAuthHelper.registerAndGetToken("unfollow_follower_user");
        var targetId = TestAuthHelper.registerAndGetUserId("unfollow_target_user");

        given()
            .header("Authorization", "Bearer " + followerToken)
        .when().post("/api/users/" + targetId + "/follow")
        .then().statusCode(200);

        given()
            .header("Authorization", "Bearer " + followerToken)
        .when().delete("/api/users/" + targetId + "/follow")
        .then().statusCode(204);
    }

    @Test
    void searchUsers_returns200() {
        TestAuthHelper.registerAndGetToken("search_target_user");

        given()
            .queryParam("q", "search_target")
        .when().get("/api/users/search")
        .then()
            .statusCode(200)
            .body("$", not(empty()));
    }
}
