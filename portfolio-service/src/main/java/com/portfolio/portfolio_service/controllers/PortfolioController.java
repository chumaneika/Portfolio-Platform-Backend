package com.portfolio.portfolio_service.controllers;

import com.portfolio.portfolio_service.dto.request.PortfolioRequest;
import com.portfolio.portfolio_service.dto.response.PortfolioResponse;
import com.portfolio.portfolio_service.services.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios/me")
@RequiredArgsConstructor
public class PortfolioController {

	private final PortfolioService portfolioService;

	@PostMapping
	public ResponseEntity<PortfolioResponse> createPortfolio(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PortfolioRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		var response = portfolioService.createPortfolio(accountId, request);
		var location = UriComponentsBuilder.fromPath("/api/v1/portfolios/me")
				.pathSegment(response.id().toString())
				.build()
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public ResponseEntity<List<PortfolioResponse>> getPortfolios(@AuthenticationPrincipal Jwt jwt) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(portfolioService.getPortfolios(accountId));
	}

	@GetMapping("/{portfolioId}")
	public ResponseEntity<PortfolioResponse> getPortfolio(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(portfolioService.getPortfolio(accountId, portfolioId));
	}

	@PutMapping("/{portfolioId}")
	public ResponseEntity<PortfolioResponse> updatePortfolio(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId,
			@Valid @RequestBody PortfolioRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(portfolioService.updatePortfolio(accountId, portfolioId, request));
	}

	@DeleteMapping("/{portfolioId}")
	public ResponseEntity<Void> deletePortfolio(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId) {
		var accountId = UUID.fromString(jwt.getSubject());
		portfolioService.deletePortfolio(accountId, portfolioId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{portfolioId}/publish")
	public ResponseEntity<PortfolioResponse> publishPortfolio(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(portfolioService.publishPortfolio(accountId, portfolioId));
	}

	@PostMapping("/{portfolioId}/draft")
	public ResponseEntity<PortfolioResponse> movePortfolioToDraft(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(portfolioService.movePortfolioToDraft(accountId, portfolioId));
	}

	@PostMapping("/{portfolioId}/archive")
	public ResponseEntity<PortfolioResponse> archivePortfolio(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(portfolioService.archivePortfolio(accountId, portfolioId));
	}
}
