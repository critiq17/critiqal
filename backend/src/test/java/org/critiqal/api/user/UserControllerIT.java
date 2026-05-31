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
        var response = TestAuthHelper.registerAndGetResponse("follow_self_user");
        var selfId = UUID.fromString(response.jsonPath().getString("id"));
        var sid = response.getCookie(TestAuthHelper.COOKIE);

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
    void getStats_freshUser_returnsZeros() {
        var userId = TestAuthHelper.registerAndGetUserId("stats_fresh_user");

        given()
        .when().get("/api/users/" + userId + "/stats")
        .then()
            .statusCode(200)
            .body("postsCount", equalTo(0))
            .body("followersCount", equalTo(0))
            .body("followingCount", equalTo(0));
    }

    @Test
    void getStats_afterFollow_reflectsRelationship() {
        var followerSid = TestAuthHelper.registerAndGetSessionCookie("stats_follower");
        var targetId = TestAuthHelper.registerAndGetUserId("stats_target");

        given()
            .cookie(TestAuthHelper.COOKIE, followerSid)
        .when().post("/api/users/" + targetId + "/follow")
        .then().statusCode(200);

        given()
        .when().get("/api/users/" + targetId + "/stats")
        .then()
            .statusCode(200)
            .body("followersCount", equalTo(1));
    }

    @Test
    void getStats_unknownUser_returns404() {
        given()
        .when().get("/api/users/" + uuid(404) + "/stats")
        .then().statusCode(404);
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

    @Test
    void getProfile_authenticated_includesStats() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("profile_stats_user");

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
        .when().get("/api/users/profile_stats_user")
        .then()
            .statusCode(200)
            .body("stats.postsCount", equalTo(0))
            .body("stats.followersCount", equalTo(0))
            .body("stats.followingCount", equalTo(0));
    }

    @Test
    void getProfile_viewer_includesIsFollowing() {
        var aResponse = TestAuthHelper.registerAndGetResponse("isfollow_user_a");
        var aSid = aResponse.getCookie(TestAuthHelper.COOKIE);
        var bId = TestAuthHelper.registerAndGetUserId("isfollow_user_b");

        // A follows B
        given()
            .cookie(TestAuthHelper.COOKIE, aSid)
        .when().post("/api/users/" + bId + "/follow")
        .then().statusCode(200);

        // Viewing B's profile as A — isFollowing must be true
        given()
            .cookie(TestAuthHelper.COOKIE, aSid)
        .when().get("/api/users/isfollow_user_b")
        .then()
            .statusCode(200)
            .body("isFollowing", equalTo(true));

        // Viewing A's own profile — isFollowing must be null (own profile)
        given()
            .cookie(TestAuthHelper.COOKIE, aSid)
        .when().get("/api/users/isfollow_user_a")
        .then()
            .statusCode(200)
            .body("isFollowing", nullValue());
    }

    @Test
    void checkFollow_returns200() {
        var aResponse = TestAuthHelper.registerAndGetResponse("checkfollow_user_a");
        var aSid = aResponse.getCookie(TestAuthHelper.COOKIE);
        var bResponse = TestAuthHelper.registerAndGetResponse("checkfollow_user_b");
        var bId = UUID.fromString(bResponse.jsonPath().getString("id"));

        // A follows B
        given()
            .cookie(TestAuthHelper.COOKIE, aSid)
        .when().post("/api/users/" + bId + "/follow")
        .then().statusCode(200);

        given()
            .cookie(TestAuthHelper.COOKIE, aSid)
        .when().get("/api/users/" + bId + "/follow/check")
        .then()
            .statusCode(200)
            .body("isFollowing", equalTo(true));
    }

    private UUID uuid(int value) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(value));
    }
}
