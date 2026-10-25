package com.portfolio.portfolio_service.repositories;

import com.portfolio.portfolio_service.dto.response.ProjectTechnologyResponse;
import com.portfolio.portfolio_service.entites.ProjectTechnology;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectTechnologyRepository {

	int nextDisplayOrder(UUID projectId);

	boolean existsByCode(UUID projectId, String technologyCode);

	ProjectTechnology save(ProjectTechnology technology);

	Optional<ProjectTechnology> findByCode(UUID projectId, String technologyCode);

	Optional<ProjectTechnologyResponse> findResponseByCode(UUID projectId, String technologyCode);

	List<ProjectTechnologyResponse> findAllResponsesByProjectId(UUID projectId);

	void flush();

	void delete(ProjectTechnology technology);

	void shiftDisplayOrdersAfter(UUID projectId, int removedDisplayOrder);
}
