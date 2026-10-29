package com.portfolio.portfolio_service.repositories.jpa;

import com.portfolio.portfolio_service.dto.response.PortfolioResponse;
import com.portfolio.portfolio_service.entites.Portfolio;
import com.portfolio.portfolio_service.entites.PortfolioStatus;
import com.portfolio.portfolio_service.repositories.PortfolioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PortfolioJpaRepository implements PortfolioRepository {

	private static final String RESPONSE_PROJECTION = """
			select new com.portfolio.portfolio_service.dto.response.PortfolioResponse(
				portfolio.id,
				portfolio.slug,
				portfolio.title,
				portfolio.description,
				portfolio.status,
				portfolio.publishedAt,
				portfolio.createdAt,
				portfolio.updatedAt
			)
			from Portfolio portfolio
			""";

	private final EntityManager entityManager;

	@Override
	public boolean existsBySlug(String slug) {
		return entityManager.createQuery(
				"select count(portfolio) from Portfolio portfolio where portfolio.slug = :slug",
				Long.class)
				.setParameter("slug", slug)
				.getSingleResult() > 0;
	}

	@Override
	public Portfolio save(Portfolio portfolio) {
		entityManager.persist(portfolio);
		entityManager.flush();
		return portfolio;
	}

	@Override
	public Optional<Portfolio> findOwnedById(UUID portfolioId, UUID accountId) {
		return entityManager.createQuery(
				"""
						select portfolio from Portfolio portfolio
						where portfolio.id = :portfolioId and portfolio.ownerAccountId = :accountId
						""",
				Portfolio.class)
				.setParameter("portfolioId", portfolioId)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<PortfolioResponse> findResponseById(UUID portfolioId, UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where portfolio.id = :portfolioId and portfolio.ownerAccountId = :accountId
						""",
				PortfolioResponse.class)
				.setParameter("portfolioId", portfolioId)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<PortfolioResponse> findPublishedResponseBySlug(String slug) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where portfolio.slug = :slug and portfolio.status = :status
						""",
				PortfolioResponse.class)
				.setParameter("slug", slug)
				.setParameter("status", PortfolioStatus.PUBLISHED)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<PortfolioResponse> findPublishedResponsesByAccountId(UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where portfolio.ownerAccountId = :accountId and portfolio.status = :status
						order by portfolio.publishedAt desc, portfolio.id
						""",
				PortfolioResponse.class)
				.setParameter("accountId", accountId)
				.setParameter("status", PortfolioStatus.PUBLISHED)
				.getResultList();
	}

	@Override
	public List<PortfolioResponse> findAllResponsesByAccountId(UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where portfolio.ownerAccountId = :accountId
						order by portfolio.createdAt, portfolio.id
						""",
				PortfolioResponse.class)
				.setParameter("accountId", accountId)
				.getResultList();
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void delete(Portfolio portfolio) {
		entityManager.remove(portfolio);
		entityManager.flush();
	}
}
