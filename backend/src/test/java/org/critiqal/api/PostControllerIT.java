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
        var token = TestAuthHelper.registerAndGetToken("post_creator");

        given()
            .header("Authorization", "Bearer " + token)
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
        var token = TestAuthHelper.registerAndGetToken("post_blank");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(JSON)
                .body("{\"content\":\"\"}")
                .when().post("/api/posts")
                .then().statusCode(400);
    }

    @Test
    void deletePost_notOwner_returns400() {
        var ownerToken = TestAuthHelper.registerAndGetToken("post_owner");
        var otherToken = TestAuthHelper.registerAndGetToken("post_other");

        var postId = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(JSON)
                .body("{\"content\":\"to be deleted\"}")
                .when().post("/api/posts")
                .then().statusCode(201)
                .extract().path("id").toString();
        given()
                .header("Authorization", "Bearer " + otherToken)
                .when().delete("/api/posts/" + postId)
                .then().statusCode(400);
    }

    @Test
    void deletePost_owner_returns204() {
        var token = TestAuthHelper.registerAndGetToken("post_delete_owner");

        var postId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(JSON)
                .body("{\"content\":\"delete me\"}")
                .when().post("/api/posts")
                .then().statusCode(201)
                .extract().path("id").toString();
        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/api/posts/" + postId)
                .then().statusCode(204);
    }
}
