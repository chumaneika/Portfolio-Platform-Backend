package com.portfolio.identity_service.repositories.springdata;

import com.portfolio.identity_service.entities.RefreshTokenEntity;
import com.portfolio.identity_service.repositories.RefreshTokenRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface RefreshTokenDataRepository extends JpaRepository<RefreshTokenEntity, UUID>, RefreshTokenRepository {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select token from RefreshTokenEntity token join fetch token.accountEntity where token.tokenHash = :tokenHash")
	Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Override
	@Modifying
	@Query("""
			update RefreshTokenEntity token
			set token.revokedAt = :revokedAt
			where token.accountEntity.id = :accountId
			  and token.revokedAt is null
			""")
	void revokeAllForAccount(@Param("accountId") UUID accountId, @Param("revokedAt") OffsetDateTime revokedAt);
}
