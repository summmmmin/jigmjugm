package com.jigmjugm.security;

import com.jigmjugm.user.entity.UserAccount;
import com.jigmjugm.user.repository.UserAccountRepository;
import com.jigmjugm.security.dto.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        UserAccount userAccount = processOAuth2User(registrationId, attributes);
        
        return new CustomOAuth2User(userAccount, attributes);
    }

    private UserAccount processOAuth2User(String registrationId, Map<String, Object> attributes) {
        if (!"kakao".equals(registrationId)) {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }

        String providerId = String.valueOf(attributes.get("id"));
        
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        String nickname = (String) properties.get("nickname");
        
        return userAccountRepository.findByProviderAndProviderUserId("KAKAO", providerId)
                .map(existingUser -> {
                    existingUser.updateNickname(nickname);
                    return userAccountRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    UserAccount newUser = UserAccount.builder()
                            .provider("KAKAO")
                            .providerUserId(providerId)
                            .nickname(nickname)
                            .build();
                    return userAccountRepository.save(newUser);
                });
    }
}