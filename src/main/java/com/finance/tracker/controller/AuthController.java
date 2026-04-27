package com.finance.tracker.controller;

import com.finance.tracker.auth.AuthContext;
import com.finance.tracker.auth.AuthInterceptor;
import com.finance.tracker.auth.AuthSessionService;
import com.finance.tracker.dto.request.AuthLoginRequest;
import com.finance.tracker.dto.response.AuthLoginResponse;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthSessionService authSessionService;
    private final UserService userService;

    public AuthController(AuthSessionService authSessionService, UserService userService) {
        this.authSessionService = authSessionService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthSessionService.AuthSession session = authSessionService.createSession(request.getEmail());
        UserResponse user = userService.findById(session.user().getId());
        return ResponseEntity.ok(new AuthLoginResponse(session.token(), user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = AuthInterceptor.AUTH_TOKEN_HEADER, required = false) String token) {
        authSessionService.invalidate(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userService.findById(AuthContext.getCurrentUserId()));
    }
}
