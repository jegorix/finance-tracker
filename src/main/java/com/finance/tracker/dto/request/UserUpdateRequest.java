package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request body for updating a user")
public class UserUpdateRequest {

    @NotBlank(message = "must not be blank")
    @Size(min = 3, max = 50)
    private String username;

    @Email
    @Size(max = 255)
    private String email;
}
