package com.skillmatch.backend.exception;

import com.skillmatch.backend.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/jobs"));

    @Test
    void handleGlobal_unexpectedException_returns500WithoutInternalDetails() {
        ResponseEntity<?> response = handler.handleGlobal(
                new IOException("/var/data/skillmatch/secret.txt: permission denied"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(messageOf(response))
                .isEqualTo(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE)
                .doesNotContain("secret.txt");
    }

    @Test
    void handleDataAccess_databaseFailure_returns500WithoutInfrastructureDetails() {
        ResponseEntity<?> response = handler.handleDataAccess(new DataAccessResourceFailureException(
                "Timed out after 30000 ms while waiting to connect to mongo-prod.internal:27017"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(messageOf(response))
                .isEqualTo(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE)
                .doesNotContain("mongo-prod.internal");
    }

    @Test
    void handleDuplicateKey_uniqueIndexViolation_returns409WithoutIndexDetails() {
        ResponseEntity<?> response = handler.handleDuplicateKey(new DuplicateKeyException(
                "E11000 duplicate key error collection: skillmatch.users index: email_1"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(messageOf(response))
                .isEqualTo(GlobalExceptionHandler.DUPLICATE_KEY_MESSAGE)
                .doesNotContain("email_1");
    }

    @Test
    void exceptionResolution_mostSpecificHandlerWins() {
        // Spring elige el handler cuyo tipo declarado está más cerca de la excepción lanzada.
        ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);

        assertThat(resolver.resolveMethodByThrowable(new DuplicateKeyException("dup")).getName())
                .isEqualTo("handleDuplicateKey");
        assertThat(resolver.resolveMethodByThrowable(new DataAccessResourceFailureException("down")).getName())
                .isEqualTo("handleDataAccess");
        assertThat(resolver.resolveMethodByThrowable(new IllegalStateException("negocio")).getName())
                .isEqualTo("handleRuntime");
        assertThat(resolver.resolveMethodByThrowable(new IOException("io")).getName())
                .isEqualTo("handleGlobal");
    }

    private static String messageOf(ResponseEntity<?> response) {
        return ((MessageResponse) response.getBody()).getMessage();
    }
}
