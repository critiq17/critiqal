package org.critiqal.domain.shared.exception;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionMapperTest {

    private final GlobalExceptionMapper mapper = new GlobalExceptionMapper();
    private final IllegalArgumentExceptionMapper illegalArgumentMapper = new IllegalArgumentExceptionMapper();

    @Test
    void mapsNotFoundTo404() {
        var response = mapper.toResponse(new NotFoundException("User not found"));

        assertResponse(response, 404, "User not found");
    }

    @Test
    void mapsConflictTo409() {
        var response = mapper.toResponse(new ConflictException("Already exists"));

        assertResponse(response, 409, "Already exists");
    }

    @Test
    void mapsForbiddenTo403() {
        var response = mapper.toResponse(new ForbiddenException("Forbidden"));

        assertResponse(response, 403, "Forbidden");
    }

    @Test
    void mapsIllegalArgumentTo400() {
        var response = illegalArgumentMapper.toResponse(new IllegalArgumentException("Bad input"));

        assertResponse(response, 400, "Bad input");
    }

    private void assertResponse(Response response, int status, String message) {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getEntity()).isEqualTo(java.util.Map.of("error", message));
    }
}
