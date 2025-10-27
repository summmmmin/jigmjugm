package com.jigmjugm.auth.service;

import com.jigmjugm.auth.KakaoOAuthClient;
import com.jigmjugm.auth.domain.RefreshToken;
import com.jigmjugm.auth.dto.AuthResponse;
import com.jigmjugm.auth.repo.RefreshTokenRepository;
import com.jigmjugm.user.dto.UserProfileResponse;
import com.jigmjugm.security.JwtTokenProvider;
import com.jigmjugm.user.entity.UserAccount;
import com.jigmjugm.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final KakaoOAuthClient kakao;
    private final UserAccountRepository userRepo;
    private final JwtTokenProvider jwt;
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${app.jwt.access-exp-seconds:3600}")
    private long accessExpSeconds;

    @Value("${app.jwt.refresh-exp-seconds:2592000}")
    private long refreshExpSeconds;

    @Transactional
    public AuthResponse exchangeKakaoToken(String kakaoAccessToken) {
        var profile = kakao.getProfile(kakaoAccessToken); // 401/5xx 처리
        String providerId = String.valueOf(profile.getId());

        boolean isNew = false;

        UserAccount user = userRepo.findByProviderAndProviderUserId("KAKAO", providerId)
                .orElse(null);

        if (user == null) {
            isNew = true;
            user = userRepo.save(UserAccount.builder()
                    .provider("KAKAO")
                    .providerUserId(providerId)
                    .nickname(profile.nickname())
                    .build());
        } else {
            // (정책) 탈퇴 계정 로그인 차단하려면 여기서 예외 던지기
            // if (user.isDeleted()) throw new AccountWithdrawnException();
            user.updateNickname(profile.nickname()); // 트랜잭션 안이면 save 불필요
        }

        String at = jwt.generateAccessToken(user);
        String rt = jwt.generateRefreshToken(user.getUserId());

        RefreshToken rtRow = new RefreshToken();
        rtRow.setUserId(user.getUserId());
        rtRow.setToken(rt);
        rtRow.setExpiresAt(Instant.now().plusSeconds(refreshExpSeconds));
        refreshTokenRepository.save(rtRow);

        return AuthResponse.builder()
                .accessToken(at)
                .refreshToken(rt)
                .refreshTokenExpiresIn((int) refreshExpSeconds)
                .tokenType("Bearer")
                .expiresIn((int) accessExpSeconds)
                .isNewUser(isNew)
                .user(UserProfileResponse.builder()
                        .userId(user.getUserId())
                        .nickname(user.getNickname())
                        .provider(user.getProvider())
                        .createdAt(user.getCreatedAt())
                        .challengeCount(0)
                        .completedChallengeCount(0)
                        .points(0)
                        .level(1)
                        .build())
                .build();
    }

    @Transactional
    public void logout(Long userId, String refreshToken, boolean allDevices) {
        if (allDevices) {
            refreshTokenRepository.deleteAllByUserId(userId);
            return;
        }
        var row = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("리프레시 토큰 없음"));
        if (!Objects.equals(row.getUserId(), userId)) {
            throw new SecurityException("토큰 소유자가 아님");
        }
        refreshTokenRepository.delete(row);
    }
}

