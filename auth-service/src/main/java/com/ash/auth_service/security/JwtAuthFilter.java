package com.ash.auth_service.security;

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

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtUtil.extractAllClaims(token);
                String userId = claims.getSubject();
                

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

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, null, authorities
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                
            } catch (io.jsonwebtoken.JwtException e) {
               
            }
        }

        filterChain.doFilter(request, response);
    }

}
