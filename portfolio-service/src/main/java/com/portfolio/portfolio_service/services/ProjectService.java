package com.portfolio.portfolio_service.services;

import com.portfolio.portfolio_service.dto.request.ProjectRequest;
import com.portfolio.portfolio_service.dto.response.ProjectResponse;
import com.portfolio.portfolio_service.entites.Project;
import com.portfolio.portfolio_service.repositories.PortfolioRepository;
import com.portfolio.portfolio_service.repositories.ProjectRepository;
import com.portfolio.portfolio_service.services.exceptions.ProjectServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

	private final PortfolioRepository portfolioRepository;
	private final ProjectRepository projectRepository;

	@Transactional
	public ProjectResponse createProject(UUID accountId, UUID portfolioId, ProjectRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var portfolio = portfolioRepository.findOwnedById(portfolioId, accountId)
				.orElseThrow(ProjectServiceException::portfolioNotFound);
		var slug = normalizeSlug(request.slug());
		if (projectRepository.existsBySlug(portfolioId, slug)) {
			throw ProjectServiceException.slugAlreadyExists();
		}

		var project = new Project(
				portfolio,
				slug,
				request.title(),
				projectRepository.nextDisplayOrder(portfolioId));
		applyRequest(project, request, slug);
		try {
			projectRepository.save(project);
		} catch (DataIntegrityViolationException exception) {
			throw ProjectServiceException.slugAlreadyExists();
		}
		return findResponse(project.getId(), portfolioId);
	}

	@Transactional(readOnly = true)
	public List<ProjectResponse> getProjects(UUID accountId, UUID portfolioId) {
		portfolioRepository.findOwnedById(portfolioId, accountId)
				.orElseThrow(ProjectServiceException::portfolioNotFound);
		return projectRepository.findAllResponsesByPortfolioId(portfolioId);
	}

	@Transactional
	public ProjectResponse updateProject(
			UUID accountId,
			UUID portfolioId,
			UUID projectId,
			ProjectRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		portfolioRepository.findOwnedById(portfolioId, accountId)
				.orElseThrow(ProjectServiceException::portfolioNotFound);
		var project = projectRepository.findByIdAndPortfolioId(projectId, portfolioId)
				.orElseThrow(ProjectServiceException::projectNotFound);
		var slug = normalizeSlug(request.slug());
		if (!project.getSlug().equals(slug) && projectRepository.existsBySlug(portfolioId, slug)) {
			throw ProjectServiceException.slugAlreadyExists();
		}

		applyRequest(project, request, slug);
		try {
			projectRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw ProjectServiceException.slugAlreadyExists();
		}
		return findResponse(projectId, portfolioId);
	}

	@Transactional
	public void deleteProject(UUID accountId, UUID portfolioId, UUID projectId) {
		portfolioRepository.findOwnedById(portfolioId, accountId)
				.orElseThrow(ProjectServiceException::portfolioNotFound);
		var project = projectRepository.findByIdAndPortfolioId(projectId, portfolioId)
				.orElseThrow(ProjectServiceException::projectNotFound);
		var removedDisplayOrder = project.getDisplayOrder();

		projectRepository.delete(project);
		projectRepository.shiftDisplayOrdersAfter(portfolioId, removedDisplayOrder);
	}

	private ProjectResponse findResponse(UUID projectId, UUID portfolioId) {
		return projectRepository.findResponseById(projectId, portfolioId)
				.orElseThrow(ProjectServiceException::projectNotFound);
	}

	private static void applyRequest(Project project, ProjectRequest request, String slug) {
		project.changeSlug(slug);
		project.updateContent(request.title(), request.summary(), request.description());
		project.updateLinks(request.sourceCodeUrl(), request.liveUrl(), request.coverImageUrl());
		try {
			project.setTimeline(request.startedOn(), request.completedOn());
		} catch (IllegalArgumentException exception) {
			throw ProjectServiceException.invalidTimeline(exception.getMessage());
		}
		project.markFeatured(Objects.requireNonNull(request.featured(), "featured must not be null"));
	}

	private static String normalizeSlug(String value) {
		return Objects.requireNonNull(value, "slug must not be null").trim().toLowerCase(Locale.ROOT);
	}
}
