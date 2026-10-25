package com.portfolio.portfolio_service.repositories.jpa;

import com.portfolio.portfolio_service.dto.response.ProjectTechnologyResponse;
import com.portfolio.portfolio_service.entites.ProjectTechnology;
import com.portfolio.portfolio_service.repositories.ProjectTechnologyRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProjectTechnologyJpaRepository implements ProjectTechnologyRepository {

	private static final String RESPONSE_PROJECTION = """
			select new com.portfolio.portfolio_service.dto.response.ProjectTechnologyResponse(
				technology.id.technologyCode,
				technology.displayName,
				technology.displayOrder
			)
			from ProjectTechnology technology
			""";

	private final EntityManager entityManager;

	@Override
	public int nextDisplayOrder(UUID projectId) {
		return entityManager.createQuery("""
				select coalesce(max(technology.displayOrder), -1) + 1
				from ProjectTechnology technology
				where technology.project.id = :projectId
				""", Integer.class)
				.setParameter("projectId", projectId)
				.getSingleResult();
	}

	@Override
	public boolean existsByCode(UUID projectId, String technologyCode) {
		return entityManager.createQuery("""
				select count(technology)
				from ProjectTechnology technology
				where technology.project.id = :projectId
				  and technology.id.technologyCode = :technologyCode
				""", Long.class)
				.setParameter("projectId", projectId)
				.setParameter("technologyCode", technologyCode)
				.getSingleResult() > 0;
	}

	@Override
	public ProjectTechnology save(ProjectTechnology technology) {
		entityManager.persist(technology);
		entityManager.flush();
		return technology;
	}

	@Override
	public Optional<ProjectTechnology> findByCode(UUID projectId, String technologyCode) {
		return entityManager.createQuery("""
				select technology from ProjectTechnology technology
				where technology.project.id = :projectId
				  and technology.id.technologyCode = :technologyCode
				""", ProjectTechnology.class)
				.setParameter("projectId", projectId)
				.setParameter("technologyCode", technologyCode)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<ProjectTechnologyResponse> findResponseByCode(UUID projectId, String technologyCode) {
		return entityManager.createQuery(RESPONSE_PROJECTION + """
				where technology.project.id = :projectId
				  and technology.id.technologyCode = :technologyCode
				""", ProjectTechnologyResponse.class)
				.setParameter("projectId", projectId)
				.setParameter("technologyCode", technologyCode)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<ProjectTechnologyResponse> findAllResponsesByProjectId(UUID projectId) {
		return entityManager.createQuery(RESPONSE_PROJECTION + """
				where technology.project.id = :projectId
				order by technology.displayOrder, technology.id.technologyCode
				""", ProjectTechnologyResponse.class)
				.setParameter("projectId", projectId)
				.getResultList();
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void delete(ProjectTechnology technology) {
		entityManager.remove(technology);
		entityManager.flush();
	}

	@Override
	public void shiftDisplayOrdersAfter(UUID projectId, int removedDisplayOrder) {
		entityManager.createQuery("""
				update ProjectTechnology technology
				set technology.displayOrder = technology.displayOrder - 1
				where technology.project.id = :projectId
				  and technology.displayOrder > :removedDisplayOrder
				""")
				.setParameter("projectId", projectId)
				.setParameter("removedDisplayOrder", removedDisplayOrder)
				.executeUpdate();
	}
}
