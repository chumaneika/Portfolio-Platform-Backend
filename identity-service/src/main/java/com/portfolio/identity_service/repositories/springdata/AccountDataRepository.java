package com.portfolio.identity_service.repositories.springdata;

import com.portfolio.identity_service.entities.AccountEntity;
import com.portfolio.identity_service.repositories.AccountRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountDataRepository extends JpaRepository<AccountEntity, UUID>, AccountRepository {

	Optional<AccountEntity> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);
}
