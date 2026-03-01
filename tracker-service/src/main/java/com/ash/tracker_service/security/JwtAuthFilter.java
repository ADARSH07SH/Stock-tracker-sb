package com.ash.tracker_service.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.extractAllClaims(token);

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            

            Object rolesObj = claims.get("roles");
            java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
            
            if (rolesObj instanceof java.util.Collection) {
                for (Object roleObj : (java.util.Collection<?>) rolesObj) {
                    final String roleStr = roleObj.toString();

                    authorities.add(() -> roleStr);
                }
            } else if (rolesObj instanceof String) {
                final String roleStr = (String) rolesObj;
                authorities.add(() -> roleStr);
            }
            
            
            if (authorities.isEmpty()) {
                String role = claims.get("role", String.class);
                if (role != null) {
                    if (!role.startsWith("ROLE_")) {
                        role = "ROLE_" + role;
                    }
                    final String finalRole = role;
                    authorities.add(() -> finalRole);
                }
            }

            request.setAttribute("userId", userId);
            request.setAttribute("email", email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId, null, authorities
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            { "error": "TOKEN_EXPIRED" }
        """);

        } catch (io.jsonwebtoken.JwtException e) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            { "error": "INVALID_TOKEN" }
        """);
        }
    }

}