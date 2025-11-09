package com.jigmjugm.auth.service;

import com.jigmjugm.auth.KakaoOAuthClient;
import com.jigmjugm.auth.domain.RefreshToken;
import com.jigmjugm.auth.dto.AuthResponse;
import com.jigmjugm.auth.repo.RefreshTokenRepository;
import com.jigmjugm.common.error.BusinessException;
import com.jigmjugm.security.JwtTokenProvider;
import com.jigmjugm.user.dto.UserProfileResponse;
import com.jigmjugm.user.entity.UserAccount;
import com.jigmjugm.user.repository.UserAccountRepository;
import com.jigmjugm.user.service.NicknameService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

import static com.jigmjugm.common.error.ApiErrorCode.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final KakaoOAuthClient kakao;
    private final UserAccountRepository userRepo;
    private final JwtTokenProvider jwt;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NicknameService nicknameService;
    private final PasswordEncoder passwordEncoder;

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
        System.out.println(user);
        if (user == null) {
            isNew = true;
            String nickname = nicknameService.generateUniqueNickname();
            user = userRepo.save(UserAccount.builder()
                    .provider("KAKAO")
                    .providerUserId(providerId)
                    .nickname(nickname)
                    .build());
        } else {
            // (정책) 탈퇴 계정 로그인 차단하려면 여기서 예외 던지기
            //if (user.isDeleted()) throw new AccountWithdrawnException();
            //user.updateNickname(profile.nickname()); // 트랜잭션 안이면 save 불필요
            if (user.isDeleted()) throw new BusinessException(FORBIDDEN, "탈퇴한 계정입니다.");
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

    @lombok.Value
    public static class IdPwRegisterRequest{
        String username;
        String password;
    }
    @lombok.Value
    public static class IdPwLoginRequest {
        String username;
        String password;
    }
    @lombok.Value
    public static class Tokens {
        String accessToken;
        String refreshToken;
    }

    @Transactional
    public Tokens registerIdPw(String username, String rawPassword) {
        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        var nick = nicknameService.generateUniqueNickname();
        var user = UserAccount.builder()
                .nickname(nick)
                .role(UserAccount.Role.USER)
                .build();
        user.setLocalCredentials(username, passwordEncoder.encode(rawPassword));
        var saved = userRepo.save(user);

        var access  = jwt.generateAccessToken(saved);
        var refresh = jwt.generateRefreshToken(saved.getUserId());

        var refreshExp = jwt.getExpiration(refresh);

        refreshTokenRepository.deleteAllByUserId(saved.getUserId());

        var rt = new RefreshToken();
        rt.setUserId(saved.getUserId());
        rt.setToken(refresh);
        rt.setExpiresAt(refreshExp);
        rt.setRevokedAt(null);
        refreshTokenRepository.save(rt);

        return new Tokens(access, refresh);
    }


    @Transactional
    public Tokens loginIdPw(String username, String rawPassword) {
        var user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (user.isDeleted()) throw new IllegalStateException("삭제된 계정입니다.");

        if (user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        var access = jwt.generateAccessToken(user);
        var refresh = jwt.generateRefreshToken(user.getUserId());

        var refreshExp = jwt.getExpiration(refresh);

        refreshTokenRepository.deleteAllByUserId(user.getUserId());

        var rt = new RefreshToken();
        rt.setUserId(user.getUserId());
        rt.setToken(refresh);
        rt.setExpiresAt(refreshExp);
        rt.setRevokedAt(null);
        refreshTokenRepository.save(rt);

        return new Tokens(access, refresh);
    }

}

