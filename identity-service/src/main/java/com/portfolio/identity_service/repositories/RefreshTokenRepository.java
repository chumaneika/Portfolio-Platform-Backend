package com.portfolio.identity_service.repositories;

import com.portfolio.identity_service.entities.RefreshTokenEntity;

import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<RefreshTokenEntity> findByTokenHashForUpdate(String tokenHash);

}
