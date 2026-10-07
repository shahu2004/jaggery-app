package auth_service.dto;

import auth_service.entity.Role;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email,
        Role role
) {
}
