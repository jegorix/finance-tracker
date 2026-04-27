package com.finance.tracker.auth;

import com.finance.tracker.domain.User;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.exception.UnauthorizedException;
import com.finance.tracker.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

    private final UserRepository userRepository;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthSessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthSession createSession(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email.trim() + "' not found"));
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.getId());
        return new AuthSession(token, user);
    }

    public User requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Authentication token is required");
        }

        Long userId = sessions.get(token);
        if (userId == null) {
            throw new UnauthorizedException("Authentication token is invalid or expired");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
    }

    public void invalidate(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessions.remove(token);
    }

    public void invalidateAllForUser(Long userId) {
        sessions.entrySet().removeIf(entry -> userId.equals(entry.getValue()));
    }

    public record AuthSession(String token, User user) {
    }
}
