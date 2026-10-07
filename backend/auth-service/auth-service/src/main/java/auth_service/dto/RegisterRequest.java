package auth_service.dto;

import auth_service.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max=100,message="Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Size(max=100,message="Email must be at most 100 characters")
        String email,

        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10 digit Indian mobile number")
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        String password,

        Role role
) {
}
