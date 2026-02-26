package com.example.mondecole_pocket.repository;

import com.example.mondecole_pocket.entity.enums.TokenType;
import com.example.mondecole_pocket.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("SELECT rt FROM RefreshToken rt " +
            "WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUserId(Long userId, LocalDateTime now);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt " +
            "WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveSessions(Long userId, LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId")
    int revokeAllByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :threshold")
    int deleteExpiredTokens(LocalDateTime threshold);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId AND rt.tokenType = :type")
    int revokeAllByUserIdAndType(Long userId, TokenType type);
}
