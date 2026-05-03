package org.critiqal.api;

import io.quarkus.test.junit.QuarkusTest;
import org.critiqal.auth.TestAuthHelper;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class CommentControllerIT {

    private Long createPost(String sid) {
        return Long.valueOf(
            given()
                .cookie(TestAuthHelper.COOKIE, sid)
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
        var sid = TestAuthHelper.registerAndGetSessionCookie("comment_valid_user");
        var postId = createPost(sid);

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
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
        var sid = TestAuthHelper.registerAndGetSessionCookie("comment_blank_user");
        var postId = createPost(sid);

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
            .contentType(JSON)
            .body("{\"content\":\"\"}")
        .when().post("/api/posts/" + postId + "/comments")
        .then().statusCode(400);
    }

    @Test
    void addReply_toReply_returns409() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("reply_to_reply_user");
        var postId = createPost(sid);

        var commentId = Long.valueOf(
            given()
                .cookie(TestAuthHelper.COOKIE, sid)
                .contentType(JSON)
                .body("{\"content\":\"root comment\"}")
            .when().post("/api/posts/" + postId + "/comments")
            .then().statusCode(201)
            .extract().path("id").toString()
        );

        var replyId = Long.valueOf(
            given()
                .cookie(TestAuthHelper.COOKIE, sid)
                .contentType(JSON)
                .body("{\"content\":\"first reply\"}")
            .when().post("/api/posts/" + postId + "/comments/" + commentId + "/replies")
            .then().statusCode(201)
            .extract().path("id").toString()
        );

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
            .contentType(JSON)
            .body("{\"content\":\"reply to reply\"}")
        .when().post("/api/posts/" + postId + "/comments/" + replyId + "/replies")
        .then().statusCode(409);
    }

    @Test
    void deleteComment_notOwner_returns403() {
        var ownerSid = TestAuthHelper.registerAndGetSessionCookie("comment_owner_del");
        var otherSid = TestAuthHelper.registerAndGetSessionCookie("comment_other_del");
        var postId = createPost(ownerSid);

        var commentId = given()
            .cookie(TestAuthHelper.COOKIE, ownerSid)
            .contentType(JSON)
            .body("{\"content\":\"to delete\"}")
        .when().post("/api/posts/" + postId + "/comments")
        .then().statusCode(201)
        .extract().path("id").toString();

        given()
            .cookie(TestAuthHelper.COOKIE, otherSid)
        .when().delete("/api/posts/" + postId + "/comments/" + commentId)
        .then().statusCode(403);
    }

    @Test
    void deleteComment_owner_returns204() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("comment_owner_204");
        var postId = createPost(sid);

        var commentId = given()
            .cookie(TestAuthHelper.COOKIE, sid)
            .contentType(JSON)
            .body("{\"content\":\"delete me\"}")
        .when().post("/api/posts/" + postId + "/comments")
        .then().statusCode(201)
        .extract().path("id").toString();

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
        .when().delete("/api/posts/" + postId + "/comments/" + commentId)
        .then().statusCode(204);
    }

    @Test
    void getComments_returns200() {
        var sid = TestAuthHelper.registerAndGetSessionCookie("comment_get_user");
        var postId = createPost(sid);

        given()
            .cookie(TestAuthHelper.COOKIE, sid)
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
