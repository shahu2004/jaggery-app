package product_service.security;

/**
 * Identity derived only from a verified JWT, never directly from caller-supplied headers.
 */
public record ProductPrincipal(Long userId, String email, String role) {
}
