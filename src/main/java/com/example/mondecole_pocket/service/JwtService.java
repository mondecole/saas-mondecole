package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.exception.ErrorCode;
import com.example.mondecole_pocket.exception.InvalidTokenException;
import com.example.mondecole_pocket.exception.TokenExpiredException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expirationMs
    ) {
        this.signingKey = buildSigningKey(secret);
        this.expirationMs = expirationMs;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Optional<String> extractUsernameSafe(String token) {
        try {
            return Optional.ofNullable(extractUsername(token));
        } catch (RuntimeException e) {
            log.debug("JWT extractUsernameSafe failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public Long extractOrganizationId(String token) {
        return extractClaim(token, claims -> {
            Object orgId = claims.get("organizationId");
            if (orgId instanceof Integer) {
                return ((Integer) orgId).longValue();
            }
            if (orgId instanceof Long) {
                return (Long) orgId;
            }
            return null;
        });
    }

    public Optional<Long> extractOrganizationIdSafe(String token) {
        try {
            return Optional.ofNullable(extractOrganizationId(token));
        } catch (RuntimeException e) {
            log.debug("JWT extractOrganizationIdSafe failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            if (userId instanceof Long) {
                return (Long) userId;
            }
            return null;
        });
    }

    public Optional<Long> extractUserIdSafe(String token) {
        try {
            return Optional.ofNullable(extractUserId(token));
        } catch (RuntimeException e) {
            log.debug("JWT extractUserIdSafe failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    // ✅ NOUVEAU : Extraire le rôle (String)
    public String extractRole(String token) {
        return extractClaim(token, claims -> (String) claims.get("role"));
    }

    public Optional<String> extractRoleSafe(String token) {
        try {
            return Optional.ofNullable(extractRole(token));
        } catch (RuntimeException e) {
            log.debug("JWT extractRoleSafe failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getTimeUntilExpirationSeconds(String token) {
        try {
            Date exp = extractExpiration(token);
            long diffMs = exp.getTime() - System.currentTimeMillis();
            return Math.max(0, diffMs / 1000);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public boolean validateTokenStrict(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername())) {
            throw new InvalidTokenException("Invalid token", ErrorCode.INVALID_SUBJECT);
        }

        Date exp = extractExpiration(token);
        if (exp.before(new Date())) {
            throw new TokenExpiredException("Expired token", toLocalDateTime(exp));
        }

        return true;
    }

    public boolean validateTokenSoft(String token, UserDetails userDetails) {
        try {
            return validateTokenStrict(token, userDetails);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // ✅ CHANGEMENT : role en String au lieu de roles en Array
        claims.put("role", extractSingleRole(userDetails.getAuthorities()));
        return createToken(claims, userDetails.getUsername());
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        // ✅ CHANGEMENT : role en String
        claims.putIfAbsent("role", extractSingleRole(userDetails.getAuthorities()));
        return createToken(claims, userDetails.getUsername());
    }

    public String generateToken(Long userId, Long organizationId, String username,
                                Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", userId);
        claims.put("organizationId", organizationId);
        // ✅ CHANGEMENT : role en String au lieu de roles en Array
        claims.put("role", extractSingleRole(authorities));

        log.info("✅ Generating JWT: userId={}, orgId={}, username={}, role={}",
                userId, organizationId, username, claims.get("role"));

        return createToken(claims, username);
    }

    public String generateToken(Long userId, Long organizationId, String username,
                                Collection<? extends GrantedAuthority> authorities,
                                Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);

        claims.putIfAbsent("userId", userId);
        claims.putIfAbsent("organizationId", organizationId);
        // ✅ CHANGEMENT : role en String
        claims.putIfAbsent("role", extractSingleRole(authorities));

        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date(System.currentTimeMillis());
        Date exp = new Date(System.currentTimeMillis() + expirationMs);

        log.debug("Creating JWT: subject={}, iat={}, exp={}, claims={}",
                subject, now, exp, claims.keySet());

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        try {
            Claims claims = parseAllClaims(token);
            return resolver.apply(claims);

        } catch (ExpiredJwtException e) {
            LocalDateTime expiredAt = toLocalDateTime(e.getClaims().getExpiration());
            throw new TokenExpiredException("Expired token", expiredAt);

        } catch (SignatureException e) {
            throw new InvalidTokenException("Invalid token", ErrorCode.INVALID_SIGNATURE);

        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("Invalid token", ErrorCode.MALFORMED_TOKEN);

        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException("Invalid token", ErrorCode.UNSUPPORTED_TOKEN);

        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid token", ErrorCode.INVALID_TOKEN);

        } catch (Exception e) {
            throw new InvalidTokenException("Invalid token", ErrorCode.INVALID_TOKEN);
        }
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey buildSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    // ✅ NOUVELLE MÉTHODE : Extraire UN SEUL rôle en String (sans le préfixe ROLE_)
    private static String extractSingleRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(role -> role.replace("ROLE_", "")) // Enlever le préfixe ROLE_
                .orElse("USER");
    }

    // ❌ SUPPRIMÉ : Ne plus utiliser cette méthode
    // private static List<String> toRoleNames(Collection<? extends GrantedAuthority> authorities) {
    //     return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    // }
}