package com.portfolio.portfolio_service.services;

import com.portfolio.portfolio_service.dto.request.ProjectTechnologyCreateRequest;
import com.portfolio.portfolio_service.dto.request.ProjectTechnologyRequest;
import com.portfolio.portfolio_service.dto.response.ProjectTechnologyResponse;
import com.portfolio.portfolio_service.entites.Project;
import com.portfolio.portfolio_service.entites.ProjectTechnology;
import com.portfolio.portfolio_service.repositories.PortfolioRepository;
import com.portfolio.portfolio_service.repositories.ProjectRepository;
import com.portfolio.portfolio_service.repositories.ProjectTechnologyRepository;
import com.portfolio.portfolio_service.services.exceptions.ProjectTechnologyServiceException;
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
public class ProjectTechnologyService {

	private final PortfolioRepository portfolioRepository;
	private final ProjectRepository projectRepository;
	private final ProjectTechnologyRepository technologyRepository;

	@Transactional
	public ProjectTechnologyResponse createTechnology(
			UUID accountId,
			UUID portfolioId,
			UUID projectId,
			ProjectTechnologyCreateRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var project = findOwnedProject(accountId, portfolioId, projectId);
		var code = normalizeCode(request.technologyCode());
		if (technologyRepository.existsByCode(projectId, code)) {
			throw ProjectTechnologyServiceException.technologyAlreadyExists();
		}

		var technology = new ProjectTechnology(
				project, code, request.displayName(), technologyRepository.nextDisplayOrder(projectId));
		try {
			technologyRepository.save(technology);
		} catch (DataIntegrityViolationException exception) {
			throw ProjectTechnologyServiceException.technologyAlreadyExists();
		}
		return findResponse(projectId, code);
	}

	@Transactional(readOnly = true)
	public List<ProjectTechnologyResponse> getTechnologies(UUID accountId, UUID portfolioId, UUID projectId) {
		findOwnedProject(accountId, portfolioId, projectId);
		return technologyRepository.findAllResponsesByProjectId(projectId);
	}

	@Transactional
	public ProjectTechnologyResponse updateTechnology(
			UUID accountId,
			UUID portfolioId,
			UUID projectId,
			String technologyCode,
			ProjectTechnologyRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		findOwnedProject(accountId, portfolioId, projectId);
		var code = normalizeCode(technologyCode);
		var technology = technologyRepository.findByCode(projectId, code)
				.orElseThrow(ProjectTechnologyServiceException::technologyNotFound);
		technology.updatePresentation(request.displayName(), technology.getDisplayOrder());
		technologyRepository.flush();
		return findResponse(projectId, code);
	}

	@Transactional
	public void deleteTechnology(UUID accountId, UUID portfolioId, UUID projectId, String technologyCode) {
		findOwnedProject(accountId, portfolioId, projectId);
		var technology = technologyRepository.findByCode(projectId, normalizeCode(technologyCode))
				.orElseThrow(ProjectTechnologyServiceException::technologyNotFound);
		var removedDisplayOrder = technology.getDisplayOrder();
		technologyRepository.delete(technology);
		technologyRepository.shiftDisplayOrdersAfter(projectId, removedDisplayOrder);
	}

	private Project findOwnedProject(UUID accountId, UUID portfolioId, UUID projectId) {
		portfolioRepository.findOwnedById(portfolioId, accountId)
				.orElseThrow(ProjectTechnologyServiceException::portfolioNotFound);
		return projectRepository.findByIdAndPortfolioId(projectId, portfolioId)
				.orElseThrow(ProjectTechnologyServiceException::projectNotFound);
	}

	private ProjectTechnologyResponse findResponse(UUID projectId, String technologyCode) {
		return technologyRepository.findResponseByCode(projectId, technologyCode)
				.orElseThrow(ProjectTechnologyServiceException::technologyNotFound);
	}

	private static String normalizeCode(String code) {
		return Objects.requireNonNull(code, "technologyCode must not be null").trim().toLowerCase(Locale.ROOT);
	}
}
