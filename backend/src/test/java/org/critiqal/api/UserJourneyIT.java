package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.support.TestAuthHelper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserJourneyIT {

    static String sid;
    static UUID postId;
    static UUID commentId;

    @Test @Order(1)
    void step1_register() {
        sid = given()
                .contentType(JSON)
                .body("{\"username\":\"journey_user\",\"password\":\"pass123\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie(TestAuthHelper.COOKIE);

        assertNotNull(sid);
    }

    @Test @Order(2)
    void step2_createPost() {
        postId = UUID.fromString(given()
                .cookie(TestAuthHelper.COOKIE, sid)
                .contentType(JSON)
                .body("{\"content\":\"my first post\"}")
                .when().post("/api/posts")
                .then().statusCode(201)
                .extract().path("id").toString());

        assertNotNull(postId);
    }

    @Test @Order(3)
    void step3_postAppearsInFeed() {
        given()
                .when().get("/api/posts")
                .then().statusCode(200)
                .body("content.id", hasItem(postId.toString()));
    }

    @Test @Order(4)
    void step4_addComment() {
        commentId = UUID.fromString(given()
                .cookie(TestAuthHelper.COOKIE, sid)
                .contentType(JSON)
                .body("{\"content\":\"great post!\"}")
                .when().post("/api/posts/" + postId + "/comments")
                .then().statusCode(201)
                .extract().path("id").toString());

        assertNotNull(commentId);
    }

    @Test @Order(5)
    void step5_commentAppearsInPost() {
        given()
                .when().get("/api/posts/" + postId + "/comments")
                .then().statusCode(200)
                .body("id", hasItem(commentId.toString()));
    }

    @Test @Order(6)
    void step6_deletePost() {
        given()
                .cookie(TestAuthHelper.COOKIE, sid)
                .when().delete("/api/posts/" + postId)
                .then().statusCode(204);
    }

    @Test @Order(7)
    void step7_deletedPost_notInFeed() {
        given()
                .when().get("/api/posts")
                .then().statusCode(200)
                .body("content.id", not(hasItem(postId.toString())));
    }
}
