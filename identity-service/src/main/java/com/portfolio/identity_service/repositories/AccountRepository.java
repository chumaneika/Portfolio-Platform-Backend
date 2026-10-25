package com.portfolio.identity_service.repositories;

import com.portfolio.identity_service.dto.response.AccountSummaryResponse;
import com.portfolio.identity_service.entities.AccountEntity;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<AccountSummaryResponse> findAccount(UUID accountId);

	Optional<AccountEntity> findById(UUID accountId);

    Optional<AccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

	void flush();
}
