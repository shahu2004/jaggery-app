package auth_service.service;

import auth_service.dto.ProfileResponse;
import auth_service.security.User;
import auth_service.exception.ApiException;
import auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return toProfile(user);
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toProfile)
                .toList();
    }

    private ProfileResponse toProfile(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt());
    }
}
