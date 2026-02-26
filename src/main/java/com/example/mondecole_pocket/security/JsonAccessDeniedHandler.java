package com.example.mondecole_pocket.security;

import com.example.mondecole_pocket.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException, ServletException {

        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader("X-Request-Id");
        }

        ErrorInfo errorInfo = mapErrorInfo(ex, request);

        ApiError body = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN",
                errorInfo.code(),
                errorInfo.message(),
                request.getRequestURI(),
                traceId,
                errorInfo.details()
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private ErrorInfo mapErrorInfo(AccessDeniedException ex, HttpServletRequest request) {
        String exMessage = ex.getMessage();
        String path = request.getRequestURI();

        if (path.startsWith("/api/admin")) {
            return new ErrorInfo(
                    "ADMIN_ACCESS_REQUIRED",
                    "Administrator access required",
                    Map.of(
                            "requiredRole", "ROLE_ADMIN",
                            "suggestion", "This feature is reserved for administrators"
                    )
            );
        }

        if (exMessage != null && exMessage.contains("ROLE_")) {
            String requiredRole = extractRole(exMessage);
            return new ErrorInfo(
                    "INSUFFICIENT_ROLE",
                    "Insufficient role for this action",
                    Map.of(
                            "requiredRole", requiredRole,
                            "suggestion", "Contact an administrator to obtain permissions"
                    )
            );
        }

        if (exMessage != null && exMessage.toLowerCase().contains("owner")) {
            return new ErrorInfo(
                    "NOT_RESOURCE_OWNER",
                    "You do not own this resource",
                    Map.of(
                            "suggestion", "You can only modify your own resources"
                    )
            );
        }

        if (exMessage != null &&
                (exMessage.toLowerCase().contains("premium") ||
                        exMessage.toLowerCase().contains("subscription"))) {
            return new ErrorInfo(
                    "PREMIUM_REQUIRED",
                    "Premium subscription required",
                    Map.of(
                            "suggestion", "Upgrade to the premium version to access this feature",
                            "upgradeUrl", "/api/subscription/upgrade"
                    )
            );
        }

        if (exMessage != null && exMessage.toLowerCase().contains("email not verified")) {
            return new ErrorInfo(
                    "EMAIL_NOT_VERIFIED",
                    "Email not verified",
                    Map.of(
                            "suggestion", "Please check your email before accessing this feature",
                            "resendUrl", "/api/auth/resend-verification"
                    )
            );
        }

        return new ErrorInfo(
                "FORBIDDEN",
                "Access denied",
                Map.of(
                        "suggestion", "You do not have the necessary permissions"
                )
        );
    }

    private String extractRole(String message) {
        if (message.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (message.contains("ROLE_MODERATOR")) return "ROLE_MODERATOR";
        if (message.contains("ROLE_PREMIUM")) return "ROLE_PREMIUM";
        return "ROLE_UNKNOWN";
    }

    private record ErrorInfo(
            String code,
            String message,
            Map<String, Object> details
    ) {}
}