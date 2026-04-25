package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.auth.TestAuthHelper;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class CommentControllerIT {

    private Long createPost(String token) {
        return Long.valueOf(
            given()
                .header("Authorization", "Bearer " + token)
                .contentType(JSON)
                .body("{\"content\":\"post for comments\"}")
            .when().post("/api/posts")
            .then().statusCode(201)
            .extract().path("id").toString()
        );
    }

    @Test
    void addComment_noAuth_returns401() {
        given()
            .contentType(JSON)
            .body("{\"content\":\"hello\"}")
        .when().post("/api/posts/1/comments")
        .then().statusCode(401);
    }

    @Test
    void addComment_valid_returns201() {
        var token = TestAuthHelper.registerAndGetToken("comment_valid_user");
        var postId = createPost(token);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(JSON)
            .body("{\"content\":\"nice post\"}")
        .when().post("/api/posts/" + postId + "/comments")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("content", org.hamcrest.Matchers.equalTo("nice post"));
    }

    @Test
    void addComment_blankContent_returns400() {
        var token = TestAuthHelper.registerAndGetToken("comment_blank_user");
        var postId = createPost(token);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(JSON)
            .body("{\"content\":\"\"}")
        .when().post("/api/posts/" + postId + "/comments")
        .then().statusCode(400);
    }

    @Test
    void addReply_toReply_returns400() {
        var token = TestAuthHelper.registerAndGetToken("reply_to_reply_user");
        var postId = createPost(token);

        var commentId = Long.valueOf(
            given()
                .header("Authorization", "Bearer " + token)
                .contentType(JSON)
                .body("{\"content\":\"root comment\"}")
            .when().post("/api/posts/" + postId + "/comments")
            .then().statusCode(201)
            .extract().path("id").toString()
        );

        var replyId = Long.valueOf(
            given()
                .header("Authorization", "Bearer " + token)
                .contentType(JSON)
                .body("{\"content\":\"first reply\"}")
            .when().post("/api/posts/" + postId + "/comments/" + commentId + "/replies")
            .then().statusCode(201)
            .extract().path("id").toString()
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(JSON)
            .body("{\"content\":\"reply to reply\"}")
        .when().post("/api/posts/" + postId + "/comments/" + replyId + "/replies")
        .then().statusCode(400);
    }

    @Test
    void deleteComment_notOwner_returns400() {
        var ownerToken = TestAuthHelper.registerAndGetToken("comment_owner_del");
        var otherToken = TestAuthHelper.registerAndGetToken("comment_other_del");
        var postId = createPost(ownerToken);

        var commentId = given()
            .header("Authorization", "Bearer " + ownerToken)
            .contentType(JSON)
            .body("{\"content\":\"to delete\"}")
        .when().post("/api/posts/" + postId + "/comments")
        .then().statusCode(201)
        .extract().path("id").toString();

        given()
            .header("Authorization", "Bearer " + otherToken)
        .when().delete("/api/posts/" + postId + "/comments/" + commentId)
        .then().statusCode(400);
    }

    @Test
    void deleteComment_owner_returns204() {
        var token = TestAuthHelper.registerAndGetToken("comment_owner_204");
        var postId = createPost(token);

        var commentId = given()
            .header("Authorization", "Bearer " + token)
            .contentType(JSON)
            .body("{\"content\":\"delete me\"}")
        .when().post("/api/posts/" + postId + "/comments")
        .then().statusCode(201)
        .extract().path("id").toString();

        given()
            .header("Authorization", "Bearer " + token)
        .when().delete("/api/posts/" + postId + "/comments/" + commentId)
        .then().statusCode(204);
    }

    @Test
    void getComments_returns200() {
        var token = TestAuthHelper.registerAndGetToken("comment_get_user");
        var postId = createPost(token);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(JSON)
            .body("{\"content\":\"visible comment\"}")
        .when().post("/api/posts/" + postId + "/comments")
        .then().statusCode(201);

        given()
        .when().get("/api/posts/" + postId + "/comments")
        .then()
            .statusCode(200)
            .body("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty()));
    }
}
