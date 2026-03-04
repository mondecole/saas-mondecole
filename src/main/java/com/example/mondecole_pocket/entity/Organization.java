package com.example.mondecole_pocket.entity;

import com.example.mondecole_pocket.entity.enums.OrganizationType;
import com.example.mondecole_pocket.entity.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations"
        /*,,
        indexes = {
                @Index(name = "idx_org_slug", columnList = "slug", unique = true),
                @Index(name = "idx_org_invitation_code", columnList = "invitation_code", unique = true)
        }*/
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    @Column(nullable = false, length = 50, unique = true)
    private String invitationCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String website;

    @Column(length = 50)
    private String registrationNumber;

    @Column(length = 50)
    private String activityDeclarationNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private OrganizationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxUsers = 50;

    @Column(name= "max_storage_mb", nullable = false)
    @Builder.Default
    private Integer maxStorageMB = 1000;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (invitationCode == null || invitationCode.isBlank()) {
            invitationCode = generateInvitationCode();
        }
        if (slug == null || slug.isBlank()) {
            slug = generateSlug();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String generateInvitationCode() {
        String prefix = name.substring(0, Math.min(3, name.length())).toUpperCase();
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + random;
    }

    private String generateSlug() {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}