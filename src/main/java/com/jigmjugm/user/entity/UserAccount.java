package com.jigmjugm.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "provider", nullable = false, length = 16)
    @Builder.Default
    private String provider = "KAKAO";

    @Column(name = "provider_user_id", nullable = false, unique = true, length = 64)
    private String providerUserId;

    @Column(name = "nickname", nullable = false, length = 40)
    private String nickname;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateEmail(String email) {
        this.email = email;
        this.emailVerified = false;
        this.emailVerifiedAt = null;
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}