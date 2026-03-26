package com.finance.tracker.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTimingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        MDC.put(
                RequestLoggingContext.REQUEST_START_TIME_MILLIS,
                String.valueOf(System.currentTimeMillis()));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestLoggingContext.REQUEST_START_TIME_MILLIS);
        }
    }
}
