package product_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ProductJwtAuthenticationFilterTest {

    private static final String SECRET = "jaggery-local-ci-secret-change-me-2026-987654321";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validSellerTokenAuthenticatesVerifiedPrincipal() throws Exception {
        TestableFilter filter = new TestableFilter(new ProductJwtService(SECRET));
        MockHttpServletRequest request = requestWithToken(createToken(SECRET, "SELLER", 60_000));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.run(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals("ROLE_SELLER", authentication.getAuthorities().iterator().next().getAuthority());
        ProductPrincipal principal = (ProductPrincipal) authentication.getPrincipal();
        assertEquals(17L, principal.userId());
        assertEquals("seller@example.com", principal.email());
        assertEquals("SELLER", principal.role());
    }

    @Test
    void invalidSignatureDoesNotAuthenticate() throws Exception {
        TestableFilter filter = new TestableFilter(new ProductJwtService(SECRET));
        String token = createToken("some-other-secret-at-least-32-bytes-long-000", "ADMIN", 60_000);

        filter.run(requestWithToken(token), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredTokenDoesNotAuthenticate() throws Exception {
        TestableFilter filter = new TestableFilter(new ProductJwtService(SECRET));
        String token = createExpiredToken(SECRET);

        filter.run(requestWithToken(token), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void unrecognizedRoleDoesNotAuthenticate() throws Exception {
        TestableFilter filter = new TestableFilter(new ProductJwtService(SECRET));
        String token = createToken(SECRET, "SUPERUSER", 60_000);

        filter.run(requestWithToken(token), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static String createToken(String secret, String role, long durationMillis) {
        long now = System.currentTimeMillis();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("seller@example.com")
                .claim("userId", 17L)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + durationMillis))
                .signWith(key)
                .compact();
    }

    private static String createExpiredToken(String secret) {
        long expiredAt = System.currentTimeMillis() - 120_000;
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("seller@example.com")
                .claim("userId", 17L)
                .claim("role", "SELLER")
                .issuedAt(new Date(expiredAt - 60_000))
                .expiration(new Date(expiredAt))
                .signWith(key)
                .compact();
    }

    private static final class TestableFilter extends ProductJwtAuthenticationFilter {
        private TestableFilter(ProductJwtService jwtService) {
            super(jwtService);
        }

        private void run(MockHttpServletRequest request,
                         MockHttpServletResponse response,
                         FilterChain chain) throws ServletException, IOException {
            super.doFilterInternal(request, response, chain);
        }
    }
}
