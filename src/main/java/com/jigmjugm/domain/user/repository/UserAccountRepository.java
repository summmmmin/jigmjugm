package com.jigmjugm.domain.user.repository;

import com.jigmjugm.domain.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    Optional<UserAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    
    Optional<UserAccount> findByProviderUserId(String providerUserId);
    
    boolean existsByProviderUserId(String providerUserId);
}