package com.jigmjugm.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="auth_refresh_token", indexes=@Index(name="idx_rt_user", columnList="user_id"))
@Getter
@Setter
public class RefreshToken {
    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="token", nullable=false, length=512, unique=true) private String token;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    @Column(name="revoked_at") private Instant revokedAt;
    public boolean isActive() { return revokedAt==null && Instant.now().isBefore(expiresAt); }
}

