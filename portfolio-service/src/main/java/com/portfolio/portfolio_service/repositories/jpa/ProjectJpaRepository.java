package com.portfolio.portfolio_service.repositories.jpa;

import com.portfolio.portfolio_service.dto.response.ProjectResponse;
import com.portfolio.portfolio_service.entites.Project;
import com.portfolio.portfolio_service.repositories.ProjectRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProjectJpaRepository implements ProjectRepository {

	private static final String RESPONSE_PROJECTION = """
			select new com.portfolio.portfolio_service.dto.response.ProjectResponse(
				project.id,
				project.slug,
				project.title,
				project.summary,
				project.description,
				project.sourceCodeUrl,
				project.liveUrl,
				project.coverImageUrl,
				project.startedOn,
				project.completedOn,
				project.displayOrder,
				project.featured,
				project.createdAt,
				project.updatedAt
			)
			from Project project
			""";

	private final EntityManager entityManager;

	@Override
	public int nextDisplayOrder(UUID portfolioId) {
		return entityManager.createQuery(
				"""
						select coalesce(max(project.displayOrder), -1) + 1
						from Project project
						where project.portfolio.id = :portfolioId
						""",
				Integer.class)
				.setParameter("portfolioId", portfolioId)
				.getSingleResult();
	}

	@Override
	public boolean existsBySlug(UUID portfolioId, String slug) {
		return entityManager.createQuery(
				"""
						select count(project)
						from Project project
						where project.portfolio.id = :portfolioId and project.slug = :slug
						""",
				Long.class)
				.setParameter("portfolioId", portfolioId)
				.setParameter("slug", slug)
				.getSingleResult() > 0;
	}

	@Override
	public Project save(Project project) {
		entityManager.persist(project);
		entityManager.flush();
		return project;
	}

	@Override
	public Optional<Project> findByIdAndPortfolioId(UUID projectId, UUID portfolioId) {
		return entityManager.createQuery(
				"""
						select project from Project project
						where project.id = :projectId and project.portfolio.id = :portfolioId
						""",
				Project.class)
				.setParameter("projectId", projectId)
				.setParameter("portfolioId", portfolioId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<ProjectResponse> findResponseById(UUID projectId, UUID portfolioId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where project.id = :projectId and project.portfolio.id = :portfolioId
						""",
				ProjectResponse.class)
				.setParameter("projectId", projectId)
				.setParameter("portfolioId", portfolioId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<ProjectResponse> findAllResponsesByPortfolioId(UUID portfolioId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where project.portfolio.id = :portfolioId
						order by project.displayOrder, project.createdAt, project.id
						""",
				ProjectResponse.class)
				.setParameter("portfolioId", portfolioId)
				.getResultList();
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void delete(Project project) {
		entityManager.remove(project);
		entityManager.flush();
	}

	@Override
	public void shiftDisplayOrdersAfter(UUID portfolioId, int removedDisplayOrder) {
		entityManager.createQuery(
				"""
						update Project project
						set project.displayOrder = project.displayOrder - 1
						where project.portfolio.id = :portfolioId
							and project.displayOrder > :removedDisplayOrder
						""")
				.setParameter("portfolioId", portfolioId)
				.setParameter("removedDisplayOrder", removedDisplayOrder)
				.executeUpdate();
	}
}
