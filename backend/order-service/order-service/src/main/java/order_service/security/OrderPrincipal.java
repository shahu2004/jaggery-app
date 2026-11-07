package order_service.security;
/** Identity derived from a JWT whose signature and expiry have been verified. */
public record OrderPrincipal(Long userId, String email, String role) {}
