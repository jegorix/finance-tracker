package com.finance.tracker.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTimingFilterTest {

    private final RequestTimingFilter filter = new RequestTimingFilter();

    @Test
    void shouldPopulateAndClearRequestStartTimeInMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            assertThat(MDC.get(RequestLoggingContext.REQUEST_START_TIME_MILLIS)).isNotBlank();
        };

        filter.doFilter(request, response, filterChain);

        assertThat(MDC.get(RequestLoggingContext.REQUEST_START_TIME_MILLIS)).isNull();
    }
}
