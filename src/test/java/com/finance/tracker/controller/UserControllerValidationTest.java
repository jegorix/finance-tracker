package com.finance.tracker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndBudgetsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.exception.GlobalExceptionHandler;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = UserController.class,
        properties = {
            "debug=false",
            "spring.main.banner-mode=off",
            "spring.main.log-startup-info=false",
            "logging.level.root=ERROR",
            "logging.level.org.springframework=ERROR",
            "logging.level.com.finance.tracker=ERROR"
        })
@Import({
    GlobalExceptionHandler.class,
    UserControllerValidationTest.TestConfig.class
})
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnifiedValidationErrorForInvalidBody() throws Exception {
        String requestBody = """
                {
                  "username": "",
                  "email": "wrong-email"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/v1/users"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void shouldReturnUnifiedValidationErrorForInvalidPathVariable() throws Exception {
        mockMvc.perform(get("/api/v1/users/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"));
    }

    @Test
    void shouldReturnUnifiedErrorForNotFoundBusinessException() throws Exception {
        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User not found 999"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/999"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        UserService userService() {
            return new UserService() {
                @Override
                public UserResponse findById(Long id) {
                    throw new ResourceNotFoundException("User not found " + id);
                }

                @Override
                public List<UserResponse> findAll() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public UserResponse create(UserRequest request) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public UserResponse update(Long id, UserUpdateRequest user) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void delete(Long id) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public UserResponse createWithAccountsAndBudgetsTx(UserWithAccountsAndBudgetsCreateRequest request) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public UserResponse createWithAccountsAndBudgetsNoTx(
                        UserWithAccountsAndBudgetsCreateRequest request) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
