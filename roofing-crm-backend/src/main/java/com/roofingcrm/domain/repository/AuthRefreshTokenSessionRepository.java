package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.AuthRefreshTokenSession;
import com.roofingcrm.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthRefreshTokenSessionRepository extends JpaRepository<AuthRefreshTokenSession, UUID> {

    Optional<AuthRefreshTokenSession> findByTokenHash(String tokenHash);

    List<AuthRefreshTokenSession> findByUserAndRevokedAtIsNullAndExpiresAtAfter(User user, Instant now);

    List<AuthRefreshTokenSession> findByFamilyIdAndRevokedAtIsNull(UUID familyId);

    List<AuthRefreshTokenSession> findByFamilyId(UUID familyId);
}
