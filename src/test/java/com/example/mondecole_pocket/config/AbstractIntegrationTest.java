package com.example.mondecole_pocket.config;

import com.example.mondecole_pocket.security.TenantContext;
import com.example.mondecole_pocket.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)  // ← ne plante pas si pas de Docker
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // true si on est sur Jenkins CI
    private static final boolean CI =
            System.getProperty("spring.profiles.active", "").contains("ci");

    // Container démarré uniquement si pas en CI
    @Container
    static PostgreSQLContainer<?> postgres;

    static {
        if (!CI) {
            postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("mondecole_test")
                    .withUsername("postgres")
                    .withPassword("secret");
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // En CI → application-ci.properties s'applique, rien à surcharger ici
        if (CI || postgres == null) return;
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url",          postgres::getJdbcUrl);
        registry.add("spring.flyway.user",         postgres::getUsername);
        registry.add("spring.flyway.password",     postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    protected static final Long TEST_ORG_ID   = 1L;
    protected static final Long TEST_USER_ID  = 10L;
    protected static final Long OTHER_ORG_ID  = 2L;
    protected static final Long OTHER_USER_ID = 20L;

    protected String generateToken(Long userId, Long orgId, String username, String role) {
        return jwtService.generateToken(
                userId,
                orgId,
                username,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request) {
        String token = generateToken(TEST_USER_ID, TEST_ORG_ID, "teacher@test.com", "TEACHER");
        return request.header("Authorization", "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder withOtherAuth(MockHttpServletRequestBuilder request) {
        String token = generateToken(OTHER_USER_ID, OTHER_ORG_ID, "other@test.com", "TEACHER");
        return request.header("Authorization", "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder withAdminAuth(MockHttpServletRequestBuilder request) {
        String token = generateToken(TEST_USER_ID, TEST_ORG_ID, "admin@test.com", "ADMIN");
        return request.header("Authorization", "Bearer " + token);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }
}