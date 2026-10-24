package com.portfolio.identity_service.repositories.jpa;

import com.portfolio.identity_service.entities.AccountEntity;
import com.portfolio.identity_service.repositories.AccountRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Primary
@RequiredArgsConstructor
public class AccountJpaRepository implements AccountRepository {

    private final EntityManager entityManager;

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
}
