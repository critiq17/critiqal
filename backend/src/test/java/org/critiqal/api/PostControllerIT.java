package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.auth.TestAuthHelper;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PostControllerIT {

    @Test
    void getFeed_noAuth_returns200() {
        given()
                .when().get("/api/posts")
                .then().statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    void createPost_noAuth_returns401() {
        given()
                .contentType(JSON)
                .body("{\"content\":\"test\"}")
                .when().post("/api/posts")
                .then().statusCode(401);
    }

    @Test
    void createPost_valid_returns201() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("post_creator");

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
            .contentType(JSON)
            .body("{\"content\":\"hello world\"}")
        .when().post("/api/posts")
        .then()
            .statusCode(201)
            .body("content", equalTo("hello world"))
            .body("id", notNullValue());
    }

    @Test
    void createPost_blankContent_returns400() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("post_blank");

        given()
                .cookie(TestAuthHelper.COOKIE, sid)
                .contentType(JSON)
                .body("{\"content\":\"\"}")
                .when().post("/api/posts")
                .then().statusCode(400);
    }

    @Test
    void deletePost_notOwner_returns400() {
        var ownerSid = TestAuthHelper.registerAndGetSessionCookie("post_owner");
        var otherSid = TestAuthHelper.registerAndGetSessionCookie("post_other");

        var postId = given()
                .cookie(TestAuthHelper.COOKIE, ownerSid)
                .contentType(JSON)
                .body("{\"content\":\"to be deleted\"}")
                .when().post("/api/posts")
                .then().statusCode(201)
                .extract().path("id").toString();
        given()
                .cookie(TestAuthHelper.COOKIE, otherSid)
                .when().delete("/api/posts/" + postId)
                .then().statusCode(400);
    }

    @Test
    void deletePost_owner_returns204() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("post_delete_owner");

        var postId = given()
                .cookie(TestAuthHelper.COOKIE, sid)
                .contentType(JSON)
                .body("{\"content\":\"delete me\"}")
                .when().post("/api/posts")
                .then().statusCode(201)
                .extract().path("id").toString();
        given()
                .cookie(TestAuthHelper.COOKIE, sid)
                .when().delete("/api/posts/" + postId)
                .then().statusCode(204);
    }
}
