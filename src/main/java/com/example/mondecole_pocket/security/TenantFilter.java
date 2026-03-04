package com.example.mondecole_pocket.security;

import com.example.mondecole_pocket.repository.OrganizationRepository;
import com.example.mondecole_pocket.repository.UserRepository;
import com.example.mondecole_pocket.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/public") ||
                path.startsWith("/actuator") ||
                path.startsWith("/api/health") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api/benchmark") ||
                path.equals("/api/auth/user-organization");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            Long organizationId = null;

            // ════════════════════════════════════════════════════════
            // PRIORITÉ 1 : JWT (requêtes authentifiées)
            // ════════════════════════════════════════════════════════

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    organizationId = jwtService.extractOrganizationId(token);
                    if (organizationId != null) {
                        log.debug("✅ Tenant {} résolu depuis JWT", organizationId);
                    }
                } catch (Exception e) {
                    log.debug("JWT invalide ou expiré, tentative fallback...");
                }
            }

            // ════════════════════════════════════════════════════════
            // PRIORITÉ 2 : Username Lookup (uniquement pour /api/auth/login)
            // ════════════════════════════════════════════════════════

            if (organizationId == null && request.getServletPath().equals("/api/auth/login")) {
                log.info("🔍 TenantFilter: Extraction organizationId pour login");
                organizationId = extractOrganizationFromLoginBody(request, response);
                if (organizationId == null) {
                    return; // Erreur déjà envoyée
                }
                log.info("✅ TenantFilter: Tenant {} résolu depuis username lookup", organizationId);
            }

            // ════════════════════════════════════════════════════════
            // FIX : Si pas de tenant résolu → laisser passer
            // Spring Security interceptera et renverra 401 proprement
            // ════════════════════════════════════════════════════════

            if (organizationId == null) {
                log.debug("⚠️ Pas de tenant résolu pour {} — délégation à Spring Security", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // ════════════════════════════════════════════════════════
            // Vérifier que l'organisation existe et est active
            // ════════════════════════════════════════════════════════

            if (!organizationRepository.existsByIdAndActiveTrue(organizationId)) {
                log.warn("⚠️ Organisation {} inexistante ou inactive", organizationId);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid organization");
                return;
            }

            // ════════════════════════════════════════════════════════
            // Configurer le TenantContext
            // ════════════════════════════════════════════════════════

            TenantContext.setTenantId(organizationId);
            log.debug("✅ Tenant {} configuré pour {}", organizationId, request.getRequestURI());

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    private Long extractOrganizationFromLoginBody(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        try {
            if (!(request instanceof CachedBodyHttpServletRequest)) {
                log.error("❌ Request non wrappée! CachedBodyFilter n'a pas fonctionné.");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return null;
            }

            CachedBodyHttpServletRequest cachedRequest = (CachedBodyHttpServletRequest) request;
            String body = cachedRequest.getBody();

            if (body == null || body.isBlank()) {
                log.warn("⚠️ Body vide pour /api/auth/login");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                return null;
            }

            String username = extractJsonField(body, "username");

            if (username == null || username.isBlank()) {
                log.warn("⚠️ Username manquant dans le body");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
                return null;
            }

            Long orgId = userRepository.findOrganizationIdByUsername(username).orElse(null);

            if (orgId == null) {
                log.warn("⚠️ Aucune organisation trouvée pour username: {}", username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return null;
            }

            return orgId;

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'extraction du tenant: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return null;
        }
    }

    private String extractJsonField(String json, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            return m.find() ? m.group(1) : null;
        } catch (Exception e) {
            log.debug("Erreur parsing JSON field {}: {}", fieldName, e.getMessage());
            return null;
        }
    }
}