package com.portfolio.identity_service.repositories.springdata;

import com.portfolio.identity_service.dto.response.AccountSummaryResponse;
import com.portfolio.identity_service.entities.AccountEntity;
import com.portfolio.identity_service.repositories.AccountRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountDataRepository extends JpaRepository<AccountEntity, UUID>, AccountRepository {

	@Override
	@Query("""
			select new com.portfolio.identity_service.dto.response.AccountSummaryResponse(
				a.id, a.email, a.username, a.status
			)
			from AccountEntity a
			where a.id = :accountId
			""")
	Optional<AccountSummaryResponse> findAccount(@Param("accountId") UUID accountId);

	Optional<AccountEntity> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);
}
