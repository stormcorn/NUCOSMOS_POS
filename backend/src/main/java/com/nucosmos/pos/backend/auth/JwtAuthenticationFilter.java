package com.nucosmos.pos.backend.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = jwtService.parse(token);
                AuthenticatedUser user = toAuthenticatedUser(claims);
                Collection<? extends GrantedAuthority> authorities = user.roleCodes()
                        .stream()
                        .map(this::toAuthority)
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUser toAuthenticatedUser(Claims claims) {
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get("employeeCode", String.class),
                claims.get("displayName", String.class),
                claims.get("storeCode", String.class),
                claims.get("activeRole", String.class),
                extractRoleCodes(claims),
                claims.get("deviceCode", String.class)
        );
    }

    private GrantedAuthority toAuthority(String roleCode) {
        return new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase().replace('-', '_'));
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoleCodes(Claims claims) {
        Object raw = claims.get("roleCodes");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
