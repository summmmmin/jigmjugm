package com.jigmjugm.security.dto;

import com.jigmjugm.user.entity.UserAccount;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final UserAccount userAccount;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(UserAccount userAccount, Map<String, Object> attributes) {
        this.userAccount = userAccount;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return userAccount.getProviderUserId();
    }

    public Long getUserId() {
        return userAccount.getUserId();
    }

    public String getNickname() {
        return userAccount.getNickname();
    }
}