package com.example.mondecole_pocket.security;

import com.example.mondecole_pocket.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String ATTR_CODE = "auth_error_code";
    private static final String ATTR_MESSAGE = "auth_error_message";

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        String code = asString(request.getAttribute(ATTR_CODE));
        String message = asString(request.getAttribute(ATTR_MESSAGE));

        if (isBlank(code)) {
            code = mapCode(ex);
        }
        if (isBlank(message)) {
            message = mapMessage(ex, code);
        }

        String traceId = MDC.get("traceId");
        if (isBlank(traceId)) {
            traceId = request.getHeader("X-Request-Id");
        }

        ApiError body = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED",
                code,
                message,
                request.getRequestURI(),
                traceId
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String mapCode(AuthenticationException ex) {
        if (ex instanceof DisabledException) return "ACCOUNT_DISABLED";
        if (ex instanceof LockedException) return "ACCOUNT_LOCKED";
        if (ex instanceof UsernameNotFoundException) return "USER_NOT_FOUND";
        if (ex instanceof BadCredentialsException) {
            String msg = ex.getMessage();
            return isBlank(msg) ? "INVALID_CREDENTIALS" : msg;
        }
        return "UNAUTHORIZED";
    }

    private String mapMessage(AuthenticationException ex, String code) {
        return switch (code) {
            case "TOKEN_EXPIRED" -> "Expired token";
            case "INVALID_SIGNATURE", "MALFORMED_TOKEN", "UNSUPPORTED_TOKEN", "INVALID_TOKEN", "INVALID_CREDENTIALS" ->
                    "Token invalide";
            case "ACCOUNT_DISABLED" -> "Account deactivated";
            case "ACCOUNT_LOCKED" -> "Account locked";
            case "USER_NOT_FOUND" -> "User not found";
            default -> "Unauthenticated";
        };
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
