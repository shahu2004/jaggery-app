package order_service.security;
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
public class OrderJwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> ALLOWED_ROLES = Set.of("CUSTOMER", "SELLER", "ADMIN");
    private final OrderJwtService jwtService;
    public OrderJwtAuthenticationFilter(OrderJwtService jwtService) { this.jwtService = jwtService; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parseAndVerify(authorization.substring(BEARER_PREFIX.length()).trim());
                Object idClaim = claims.get("userId");
                Object roleClaim = claims.get("role");
                if (idClaim instanceof Number id && id.longValue() > 0
                        && claims.getSubject() != null && roleClaim instanceof String role
                        && ALLOWED_ROLES.contains(role)) {
                    OrderPrincipal principal = new OrderPrincipal(id.longValue(), claims.getSubject(), role);
                    var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
