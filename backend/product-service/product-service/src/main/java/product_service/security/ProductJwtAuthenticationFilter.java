package product_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ProductJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> ALLOWED_ROLES = Set.of("CUSTOMER", "SELLER", "ADMIN");

    private final ProductJwtService jwtService;

    public ProductJwtAuthenticationFilter(ProductJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null
                && authorization.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            try {
                Claims claims = jwtService.parseAndVerify(token);
                Object idClaim = claims.get("userId");
                Object roleClaim = claims.get("role");

                if (idClaim instanceof Number id
                        && id.longValue() > 0
                        && claims.getSubject() != null
                        && roleClaim instanceof String role
                        && ALLOWED_ROLES.contains(role)) {
                    ProductPrincipal principal =
                            new ProductPrincipal(id.longValue(), claims.getSubject(), role);

                    UsernamePasswordAuthenticationToken authentication =
                            UsernamePasswordAuthenticationToken.authenticated(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Protected endpoints will be rejected by Spring Security if no valid
                // authentication was established. Do not expose token validation details.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
