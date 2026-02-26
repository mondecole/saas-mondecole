package com.example.mondecole_pocket.security;

import com.example.mondecole_pocket.exception.ErrorCode;
import com.example.mondecole_pocket.exception.InvalidTokenException;
import com.example.mondecole_pocket.exception.TokenExpiredException;
import com.example.mondecole_pocket.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.mondecole_pocket.exception.ErrorCode.INVALID_TOKEN;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/benchmark");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            jwtService.validateTokenStrict(token, userDetails);

            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);

        } catch (TokenExpiredException e) {
            setAuthErrorAttributes(request, e.getErrorCode(), e.getMessage());
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(e.getErrorCode().name(), e)
            );

        } catch (InvalidTokenException e) {
            setAuthErrorAttributes(request, e.getErrorCode(), e.getMessage());
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(e.getErrorCode().name(), e)
            );

        } catch (Exception e) {
            setAuthErrorAttributes(request, INVALID_TOKEN, "Invalid token");
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("INVALID_TOKEN", e)
            );
        }
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) return null;
        if (!authorizationHeader.startsWith("Bearer ")) return null;

        String token = authorizationHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private void setAuthErrorAttributes(HttpServletRequest request, ErrorCode code, String message) {
        request.setAttribute("auth_error_code", code.name());
        request.setAttribute("auth_error_message", message);
    }
}
