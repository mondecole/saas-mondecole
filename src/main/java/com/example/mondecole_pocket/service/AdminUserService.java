package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.UserStatsResponse;
import com.example.mondecole_pocket.entity.enums.UserRole;
import com.example.mondecole_pocket.repository.CourseEnrollmentRepository;
import com.example.mondecole_pocket.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    /**
     * ❌ VERSION LENTE : N+1 Problem
     */
    @Transactional(readOnly = true)
    public Page<UserStatsResponse> getUsersWithStatsSlow(Long organizationId, UserRole role, Pageable pageable) {
        log.warn("🐌 Executing SLOW query (N+1 problem)");

        return userRepository.findByOrganizationIdAndRole(organizationId, role, pageable)
                .map(user -> {
                    int enrollmentCount = (int) enrollmentRepository
                            .countByStudentId(user.getId());

                    return new UserStatsResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getFirstName() + " " + user.getLastName(),
                            user.getRole().name(),
                            enrollmentCount
                    );
                });
    }

    @Cacheable(value = "userStats", key = "#organizationId + ':' + #role.name() + ':' + #pageable.pageNumber")
    public Page<UserStatsResponse> getUsersWithStatsFast(Long organizationId, UserRole role, Pageable pageable) {
        log.info("🚀 Executing FAST query (optimized JOIN)");
        return userRepository.findUsersWithStats(organizationId, role, pageable);
    }

}