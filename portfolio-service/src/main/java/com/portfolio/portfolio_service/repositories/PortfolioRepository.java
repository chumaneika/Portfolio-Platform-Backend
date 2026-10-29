package com.portfolio.portfolio_service.repositories;

import com.portfolio.portfolio_service.dto.response.PortfolioResponse;
import com.portfolio.portfolio_service.entites.Portfolio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {

	boolean existsBySlug(String slug);

	Portfolio save(Portfolio portfolio);

	Optional<Portfolio> findOwnedById(UUID portfolioId, UUID accountId);

	Optional<PortfolioResponse> findResponseById(UUID portfolioId, UUID accountId);

	Optional<PortfolioResponse> findPublishedResponseBySlug(String slug);

	Optional<UUID> findOwnerAccountIdById(UUID portfolioId);

	List<PortfolioResponse> findPublishedResponsesByAccountId(UUID accountId);

	List<PortfolioResponse> findAllResponsesByAccountId(UUID accountId);

	void flush();

	void delete(Portfolio portfolio);
}
