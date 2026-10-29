package com.portfolio.identity_service.repositories.jpa;

import com.portfolio.identity_service.dto.response.AccountSummaryResponse;
import com.portfolio.identity_service.dto.response.PublicAccountResponse;
import com.portfolio.identity_service.entities.AccountEntity;
import com.portfolio.identity_service.repositories.AccountRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
@RequiredArgsConstructor
public class AccountJpaRepository implements AccountRepository {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountSummaryResponse> findAccount(UUID accountId) {
        return entityManager.createQuery(
                """
                       select new com.portfolio.identity_service.dto.response.AccountSummaryResponse(
                           a.id, a.email, a.username, a.status
                       )
                       from AccountEntity a
                       where a.id = :accountId
                   """, AccountSummaryResponse.class)
                .setParameter("accountId", accountId)
                .getResultStream()
                .findFirst();
    }

	@Override
	@Transactional(readOnly = true)
	public Optional<PublicAccountResponse> findPublicAccount(UUID accountId) {
		return entityManager.createQuery(
				"""
						select new com.portfolio.identity_service.dto.response.PublicAccountResponse(a.id, a.username)
						from AccountEntity a
						where a.id = :accountId and a.status = :status
						""", PublicAccountResponse.class)
				.setParameter("accountId", accountId)
				.setParameter("status", com.portfolio.identity_service.entities.AccountStatus.ACTIVE)
				.getResultStream()
				.findFirst();
	}

    @Transactional(readOnly = true)
    public Optional<AccountEntity> findById(UUID accountId) {
        return Optional.ofNullable(entityManager.find(AccountEntity.class, accountId));
    }

    @Transactional(readOnly = true)
    public Optional<AccountEntity> findByEmail(String email) {
        return entityManager.createQuery(
                """
                      select a
                      from AccountEntity a
                      where a.email = :email
              """, AccountEntity.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return entityManager.createQuery(
                        """
                              select count(a)
                              from AccountEntity a
                              where a.email = :email
                      """, Long.class)
                .setParameter("email", email)
                .getSingleResult() > 0;
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return entityManager.createQuery(
                        """
                              select count(a)
                              from AccountEntity a
                              where a.username = :username
                      """, Long.class)
                .setParameter("username", username)
                .getSingleResult() > 0;
    }

    @Override
    public void flush() {
        entityManager.flush();
    }
}
