package com.trainbooking.accounts.repository;

import com.trainbooking.accounts.domain.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenHashAndTokenTypeAndConsumedAtIsNull(
            String tokenHash, String tokenType);
}
