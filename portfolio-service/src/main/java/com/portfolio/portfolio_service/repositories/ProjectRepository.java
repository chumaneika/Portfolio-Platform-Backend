package com.portfolio.portfolio_service.repositories;

import com.portfolio.portfolio_service.dto.response.ProjectResponse;
import com.portfolio.portfolio_service.entites.Project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

	int nextDisplayOrder(UUID portfolioId);

	boolean existsBySlug(UUID portfolioId, String slug);

	Project save(Project project);

	Optional<Project> findByIdAndPortfolioId(UUID projectId, UUID portfolioId);

	Optional<ProjectResponse> findResponseById(UUID projectId, UUID portfolioId);

	List<ProjectResponse> findAllResponsesByPortfolioId(UUID portfolioId);

	void flush();

	void delete(Project project);

	void shiftDisplayOrdersAfter(UUID portfolioId, int removedDisplayOrder);
}
