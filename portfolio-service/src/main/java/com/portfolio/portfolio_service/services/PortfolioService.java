package com.portfolio.portfolio_service.services;

import com.portfolio.portfolio_service.dto.request.PortfolioRequest;
import com.portfolio.portfolio_service.dto.response.PortfolioResponse;
import com.portfolio.portfolio_service.dto.response.PublicPortfolioResponse;
import com.portfolio.portfolio_service.dto.response.PublicProjectResponse;
import com.portfolio.portfolio_service.entites.Portfolio;
import com.portfolio.portfolio_service.entites.PortfolioStatus;
import com.portfolio.portfolio_service.repositories.PortfolioRepository;
import com.portfolio.portfolio_service.repositories.ProjectRepository;
import com.portfolio.portfolio_service.repositories.ProjectTechnologyRepository;
import com.portfolio.portfolio_service.services.exceptions.PortfolioServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {

	private final PortfolioRepository portfolioRepository;
	private final ProjectRepository projectRepository;
	private final ProjectTechnologyRepository technologyRepository;

	@Transactional
	public PortfolioResponse createPortfolio(UUID accountId, PortfolioRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var slug = normalizeSlug(request.slug());
		requireAvailableSlug(slug);
		if (portfolioRepository.existsBySlug(slug)) {
			throw PortfolioServiceException.slugAlreadyExists();
		}

		var portfolio = new Portfolio(accountId, slug, request.title(), request.description());
		try {
			portfolioRepository.save(portfolio);
		} catch (DataIntegrityViolationException exception) {
			throw PortfolioServiceException.slugAlreadyExists();
		}
		return findResponse(portfolio.getId(), accountId);
	}

	@Transactional(readOnly = true)
	public List<PortfolioResponse> getPortfolios(UUID accountId) {
		return portfolioRepository.findAllResponsesByAccountId(accountId);
	}

	@Transactional(readOnly = true)
	public PortfolioResponse getPortfolio(UUID accountId, UUID portfolioId) {
		return findResponse(portfolioId, accountId);
	}

	@Transactional
	public PortfolioResponse updatePortfolio(UUID accountId, UUID portfolioId, PortfolioRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var portfolio = portfolioRepository.findOwnedById(portfolioId, accountId)
				.orElseThrow(PortfolioServiceException::portfolioNotFound);
		var slug = normalizeSlug(request.slug());
		requireAvailableSlug(slug);
		if (!portfolio.getSlug().equals(slug) && portfolioRepository.existsBySlug(slug)) {
			throw PortfolioServiceException.slugAlreadyExists();
		}

		portfolio.changeSlug(slug);
		portfolio.updateDetails(request.title(), request.description());
		try {
			portfolioRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw PortfolioServiceException.slugAlreadyExists();
		}
		return findResponse(portfolioId, accountId);
	}

	@Transactional
	public void deletePortfolio(UUID accountId, UUID portfolioId) {
		var portfolio = portfolioRepository.findOwnedById(portfolioId, accountId)
				.orElseThrow(PortfolioServiceException::portfolioNotFound);
		portfolioRepository.delete(portfolio);
	}

	@Transactional
	public PortfolioResponse publishPortfolio(UUID accountId, UUID portfolioId) {
		var portfolio = findOwnedPortfolio(accountId, portfolioId);
		if (portfolio.getStatus() != PortfolioStatus.DRAFT) {
			throw PortfolioServiceException.invalidStatusTransition(portfolio.getStatus(), PortfolioStatus.PUBLISHED);
		}
		portfolio.publish(OffsetDateTime.now(ZoneOffset.UTC));
		portfolioRepository.flush();
		return findResponse(portfolioId, accountId);
	}

	@Transactional
	public PortfolioResponse movePortfolioToDraft(UUID accountId, UUID portfolioId) {
		var portfolio = findOwnedPortfolio(accountId, portfolioId);
		if (portfolio.getStatus() == PortfolioStatus.DRAFT) {
			throw PortfolioServiceException.invalidStatusTransition(portfolio.getStatus(), PortfolioStatus.DRAFT);
		}
		portfolio.moveToDraft();
		portfolioRepository.flush();
		return findResponse(portfolioId, accountId);
	}

	@Transactional
	public PortfolioResponse archivePortfolio(UUID accountId, UUID portfolioId) {
		var portfolio = findOwnedPortfolio(accountId, portfolioId);
		if (portfolio.getStatus() == PortfolioStatus.ARCHIVED) {
			throw PortfolioServiceException.invalidStatusTransition(portfolio.getStatus(), PortfolioStatus.ARCHIVED);
		}
		portfolio.archive();
		portfolioRepository.flush();
		return findResponse(portfolioId, accountId);
	}

	@Transactional(readOnly = true)
	public PublicPortfolioResponse getPublishedPortfolio(String slug) {
		var portfolio = portfolioRepository.findPublishedResponseBySlug(normalizeSlug(slug))
				.orElseThrow(PortfolioServiceException::portfolioNotFound);
		var projects = projectRepository.findAllResponsesByPortfolioId(portfolio.id()).stream()
				.map(project -> new PublicProjectResponse(
						project.id(),
						project.slug(),
						project.title(),
						project.summary(),
						project.description(),
						project.sourceCodeUrl(),
						project.liveUrl(),
						project.coverImageUrl(),
						project.startedOn(),
						project.completedOn(),
						project.displayOrder(),
						project.featured(),
						technologyRepository.findAllResponsesByProjectId(project.id())))
				.toList();
		return new PublicPortfolioResponse(
				portfolio.id(),
				portfolio.slug(),
				portfolio.title(),
				portfolio.description(),
				portfolio.publishedAt(),
				projects);
	}

	private Portfolio findOwnedPortfolio(UUID accountId, UUID portfolioId) {
		return portfolioRepository.findOwnedById(portfolioId, accountId)
				.orElseThrow(PortfolioServiceException::portfolioNotFound);
	}

	private PortfolioResponse findResponse(UUID portfolioId, UUID accountId) {
		return portfolioRepository.findResponseById(portfolioId, accountId)
				.orElseThrow(PortfolioServiceException::portfolioNotFound);
	}

	private static String normalizeSlug(String value) {
		return Objects.requireNonNull(value, "slug must not be null").trim().toLowerCase(Locale.ROOT);
	}

	private static void requireAvailableSlug(String slug) {
		if (slug.equals("me")) {
			throw PortfolioServiceException.invalidSlug();
		}
	}
}
