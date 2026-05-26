package org.critiqal.api.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class DeviceGuardIT {

    @Test
    void register_newDevice_returns201() {
        given()
                .contentType(JSON)
                .header("X-Device-Id", "device-guard-new-a")
                .body("{\"username\":\"device_first_user\",\"password\":\"pass123\",\"email\":\"test-fill@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201);
    }

    @Test
    void register_sameDevice_secondAccount_returns409() {
        var deviceId = "device-guard-conflict-a";

        given()
                .contentType(JSON)
                .header("X-Device-Id", deviceId)
                .body("{\"username\":\"device_conflict_1\",\"password\":\"pass123\",\"email\":\"test-fill@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201);

        given()
                .contentType(JSON)
                .header("X-Device-Id", deviceId)
                .body("{\"username\":\"device_conflict_2\",\"password\":\"pass123\",\"email\":\"test-fill@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(409)
                .body("error", equalTo("device_already_used"));
    }

    @Test
    void register_sameDevice_afterLogout_stillReturns409() {
        var deviceId = "device-guard-conflict-b";

        var sid = given()
                .contentType(JSON)
                .header("X-Device-Id", deviceId)
                .body("{\"username\":\"device_conflict_3\",\"password\":\"pass123\",\"email\":\"test-fill@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie("session");

        given()
                .contentType(JSON)
                .cookie("session", sid)
                .when().post("/api/auth/logout")
                .then().statusCode(204);

        given()
                .contentType(JSON)
                .header("X-Device-Id", deviceId)
                .body("{\"username\":\"device_conflict_4\",\"password\":\"pass123\",\"email\":\"test-fill@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(409)
                .body("error", equalTo("device_already_used"));
    }

    @Test
    void register_noDeviceId_returns201_allowedFallback() {
        given()
                .contentType(JSON)
                .body("{\"username\":\"device_no_header_user\",\"password\":\"pass123\",\"email\":\"test-fill@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201);
    }

    @Test
    void getSessions_requiresAuth() {
        given()
                .when().get("/api/auth/sessions")
                .then().statusCode(401);
    }

    @Test
    void getSessions_returnsListWithCurrentMarker() {
        var sid = given()
                .contentType(JSON)
                .header("X-Device-Id", "device-guard-sessions-a")
                .body("{\"username\":\"sessions_list_user\",\"password\":\"pass123\",\"email\":\"test-fill@test.local\"}")
                .when().post("/api/auth/register")
                .then().statusCode(201)
                .extract().cookie("session");

        given()
                .cookie("session", sid)
                .when().get("/api/auth/sessions")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].current", equalTo(true))
                .body("[0].platform", notNullValue());
    }
}
