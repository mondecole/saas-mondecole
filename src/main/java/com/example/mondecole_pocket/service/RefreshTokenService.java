package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.entity.enums.TokenType;
import com.example.mondecole_pocket.entity.RefreshToken;
import com.example.mondecole_pocket.exception.ErrorCode;
import com.example.mondecole_pocket.exception.InvalidTokenException;
import com.example.mondecole_pocket.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.security.refresh.days:30}")
    private int refreshDays;

    @Value("${app.security.refresh.remember-days:90}")
    private int rememberDays;

    @Value("${app.security.refresh.rotate:true}")
    private boolean rotate;

    @Value("${app.security.refresh.max-active-sessions:5}")
    private int maxActiveSessions;

    public record Issued(String rawToken, LocalDateTime expiresAt, TokenType type,
                         Long userId, Long organizationId) {}

    @Transactional
    public Issued issue(Long userId, Long organizationId, boolean rememberMe, HttpServletRequest request) {
        TokenType type = rememberMe ? TokenType.REMEMBER_ME : TokenType.REFRESH;
        int days = rememberMe ? rememberDays : refreshDays;

        enforceMaxSessions(userId);

        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        String hash = sha256(raw);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exp = now.plusDays(days);

        RefreshToken entity = RefreshToken.builder()
                .tokenHash(hash)
                .userId(userId)
                .organizationId(organizationId)  // ✅ AJOUT
                .tokenType(type)
                .createdAt(now)
                .lastUsedAt(now)
                .expiresAt(exp)
                .revoked(false)
                .ipAddress(extractIp(request))
                .userAgent(extractUserAgent(request))
                .deviceName(extractDeviceName(request))
                .build();

        refreshTokenRepository.save(entity);
        return new Issued(raw, exp, type, userId, organizationId);  // ✅ AJOUT
    }

    @Transactional
    public RefreshToken validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Invalid refresh token", ErrorCode.REFRESH_TOKEN_INVALID);
        }

        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token", ErrorCode.REFRESH_TOKEN_INVALID));

        if (token.isRevoked()) {
            throw new InvalidTokenException("Invalid refresh token", ErrorCode.REFRESH_TOKEN_REVOKED);
        }
        if (token.isExpired()) {
            throw new InvalidTokenException("Invalid refresh token", ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        token.markAsUsed();
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public Issued rotate(String rawToken, HttpServletRequest request) {
        RefreshToken old = validate(rawToken);

        if (!rotate) {
            // ✅ AJOUT organizationId dans le return
            return new Issued(
                    rawToken,
                    old.getExpiresAt(),
                    old.getTokenType(),
                    old.getUserId(),
                    old.getOrganizationId()  // ← AJOUT ICI
            );
        }

        old.revoke();
        refreshTokenRepository.save(old);

        boolean rememberMe = old.getTokenType() == TokenType.REMEMBER_ME;

        // ✅ AJOUT organizationId dans l'appel à issue()
        return issue(
                old.getUserId(),
                old.getOrganizationId(),  // ← AJOUT ICI
                rememberMe,
                request
        );
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;

        refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(t -> {
            t.revoke();
            refreshTokenRepository.save(t);
        });
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deleted = refreshTokenRepository.deleteExpiredTokens(threshold);
        log.info("Cleanup refresh tokens: deleted={}, threshold={}", deleted, threshold);
    }

    private void enforceMaxSessions(Long userId) {
        long active = refreshTokenRepository.countActiveSessions(userId, LocalDateTime.now());
        if (active < maxActiveSessions) return;

        List<RefreshToken> tokens = refreshTokenRepository.findValidTokensByUserId(userId, LocalDateTime.now());
        tokens.stream()
                .min((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .ifPresent(oldest -> {
                    oldest.revoke();
                    refreshTokenRepository.save(oldest);
                });
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return (ip != null && ip.contains(",")) ? ip.split(",")[0].trim() : ip;
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        String ua = request.getHeader("User-Agent");
        return (ua != null && ua.length() > 500) ? ua.substring(0, 500) : ua;
    }

    private String extractDeviceName(HttpServletRequest request) {
        if (request == null) return "Unknown";
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS Device";
        if (ua.contains("Android")) return "Android Device";
        if (ua.contains("Windows")) return "Windows PC";
        if (ua.contains("Macintosh")) return "Mac";
        if (ua.contains("Linux")) return "Linux PC";
        return "Unknown Device";
    }
}
