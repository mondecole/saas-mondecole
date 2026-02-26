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
                path.equals("/api/auth/user-organization"); // ✅ AJOUT
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

                // ✅ LOGS DE DEBUG CRITIQUES
                log.info("🔍 TenantFilter: Extraction organizationId pour login");
                log.info("🔍 TenantFilter: Request class = {}", request.getClass().getName());
                log.info("🔍 TenantFilter: Is CachedBodyHttpServletRequest? {}",
                        request instanceof CachedBodyHttpServletRequest);

                organizationId = extractOrganizationFromLoginBody(request, response);

                if (organizationId == null) {
                    return; // Erreur déjà envoyée
                }

                log.info("✅ TenantFilter: Tenant {} résolu depuis username lookup", organizationId);
            }

            // ════════════════════════════════════════════════════════
            // Si toujours null → erreur
            // ════════════════════════════════════════════════════════

            if (organizationId == null) {
                log.warn("⚠️ Tenant non résolu - Path: {}", request.getRequestURI());
                response.sendError(400, "Organization not specified");
                return;
            }

            // ════════════════════════════════════════════════════════
            // Vérifier que l'organisation existe et est active
            // ════════════════════════════════════════════════════════

            if (!organizationRepository.existsByIdAndActiveTrue(organizationId)) {
                log.warn("⚠️ Organisation {} inexistante ou inactive", organizationId);
                response.sendError(400, "Invalid organization");
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
            log.info("🔍 extractOrganizationFromLoginBody: Début");
            log.info("🔍 extractOrganizationFromLoginBody: Request type = {}",
                    request.getClass().getName());

            // ✅ Vérifier que la request est wrappée
            if (!(request instanceof CachedBodyHttpServletRequest)) {
                log.error("❌ Request non wrappée! CachedBodyFilter n'a pas fonctionné.");
                log.error("❌ Request class = {}", request.getClass().getName());
                response.sendError(500, "Internal server error");
                return null;
            }

            CachedBodyHttpServletRequest cachedRequest = (CachedBodyHttpServletRequest) request;
            String body = cachedRequest.getBody();

            log.info("🔍 Body extrait: {}", body);

            if (body == null || body.isBlank()) {
                log.warn("⚠️ Body vide pour /api/auth/login");
                response.sendError(400, "Missing request body");
                return null;
            }

            // Parse JSON pour extraire le username
            String username = extractJsonField(body, "username");

            if (username == null || username.isBlank()) {
                log.warn("⚠️ Username manquant dans le body");
                response.sendError(400, "Username is required");
                return null;
            }

            log.info("🔍 Lookup organization pour username: {}", username);

            // ✅ Requête DB : username → organizationId
            Long orgId = userRepository.findOrganizationIdByUsername(username)
                    .orElse(null);

            if (orgId == null) {
                log.warn("⚠️ Aucune organisation trouvée pour username: {}", username);
                response.sendError(401, "Invalid credentials");
                return null;
            }

            log.info("✅ Organization {} trouvée pour username: {}", orgId, username);
            return orgId;

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'extraction du tenant: {}", e.getMessage(), e);
            response.sendError(500, "Internal server error");
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