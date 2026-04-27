package com.finance.tracker.auth;

import com.finance.tracker.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConditionalOnBean(AuthSessionService.class)
public class AuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_TOKEN_HEADER = "X-Auth-Token";

    private final AuthSessionService authSessionService;

    public AuthInterceptor(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicEndpoint(request)) {
            return true;
        }

        User user = authSessionService.requireUser(request.getHeader(AUTH_TOKEN_HEADER));
        AuthContext.setCurrentUserId(user.getId());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
        Exception ex) {
        AuthContext.clear();
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return "OPTIONS".equalsIgnoreCase(method)
                || ("POST".equalsIgnoreCase(method) && "/api/v1/auth/login".equals(uri))
                || ("POST".equalsIgnoreCase(method) && "/api/v1/users".equals(uri))
                || ("POST".equalsIgnoreCase(method) && "/api/v1/users/create-accounts-and-budgets".equals(uri));
    }
}
