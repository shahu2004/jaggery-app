package auth_service.dto;

import auth_service.entity.Role;

import java.time.LocalDateTime;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        LocalDateTime createdAt
) {
}