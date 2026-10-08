package auth_service.service;

import auth_service.dto.AuthResponse;
import auth_service.dto.LoginRequest;
import auth_service.dto.RegisterRequest;
import auth_service.entity.Role;
import auth_service.security.User;
import auth_service.exception.ApiException;
import auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        if (role == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot register as ADMIN");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .build();

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}