package com.portfolio.identity_service.repositories.jpa;


import com.portfolio.identity_service.entities.RefreshTokenEntity;
import com.portfolio.identity_service.repositories.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

@Repository
@Primary
@RequiredArgsConstructor
public class RefreshTokenJpaRepository implements RefreshTokenRepository {

    private final EntityManager entityManager;

    @Transactional
    public Optional<RefreshTokenEntity> findByTokenHashForUpdate(
            String tokenHash
    ) {
        return entityManager.createQuery(
                        """
                        select token
                        from RefreshTokenEntity token
                        join fetch token.accountEntity
                        where token.tokenHash = :tokenHash
                        """, RefreshTokenEntity.class)
                .setParameter("tokenHash", tokenHash)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }

    @Override
    public void revokeAllForAccount(UUID accountId, OffsetDateTime revokedAt) {
        entityManager.createQuery("""
                        update RefreshTokenEntity token
                        set token.revokedAt = :revokedAt
                        where token.accountEntity.id = :accountId
                          and token.revokedAt is null
                        """)
                .setParameter("accountId", accountId)
                .setParameter("revokedAt", revokedAt)
                .executeUpdate();
    }
}
