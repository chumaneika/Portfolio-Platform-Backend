package com.portfolio.identity_service.repositories;

import com.portfolio.identity_service.entities.AccountEntity;

import java.util.Optional;

public interface AccountRepository {

    Optional<AccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
