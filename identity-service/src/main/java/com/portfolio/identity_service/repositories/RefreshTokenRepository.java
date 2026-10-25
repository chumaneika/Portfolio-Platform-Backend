package com.portfolio.identity_service.repositories;

import com.portfolio.identity_service.entities.RefreshTokenEntity;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshTokenEntity> findByTokenHashForUpdate(String tokenHash);

    void revokeAllForAccount(UUID accountId, OffsetDateTime revokedAt);

}
