package com.devcontext.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {
    @Test
    void returnsSafeStructuredError() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/example");

        var response = new ApiExceptionHandler().responseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Not found");
        assertThat(response.getBody().path()).isEqualTo("/api/example");
    }
}
