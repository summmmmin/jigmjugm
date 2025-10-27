package com.jigmjugm.user.repository;

import com.jigmjugm.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    Optional<UserAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    
    Optional<UserAccount> findByProviderUserId(String providerUserId);
    
    boolean existsByProviderUserId(String providerUserId);

    boolean existsByNickname(String nickname);
    Optional<UserAccount> findByNickname(String nickname);
}