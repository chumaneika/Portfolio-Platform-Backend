package com.portfolio.portfolio_service.controllers;

import com.portfolio.portfolio_service.dto.request.ProjectTechnologyCreateRequest;
import com.portfolio.portfolio_service.dto.request.ProjectTechnologyRequest;
import com.portfolio.portfolio_service.dto.response.ProjectTechnologyResponse;
import com.portfolio.portfolio_service.services.ProjectTechnologyService;
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
@RequestMapping("/api/v1/portfolios/me/{portfolioId}/projects/{projectId}/technologies")
@RequiredArgsConstructor
public class ProjectTechnologyController {

	private final ProjectTechnologyService technologyService;

	@PostMapping
	public ResponseEntity<ProjectTechnologyResponse> createTechnology(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId,
			@PathVariable UUID projectId,
			@Valid @RequestBody ProjectTechnologyCreateRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		var response = technologyService.createTechnology(accountId, portfolioId, projectId, request);
		var location = UriComponentsBuilder.fromPath("/api/v1/portfolios/me")
				.pathSegment(portfolioId.toString(), "projects", projectId.toString(), "technologies", response.technologyCode())
				.build()
				.encode()
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public ResponseEntity<List<ProjectTechnologyResponse>> getTechnologies(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId,
			@PathVariable UUID projectId) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(technologyService.getTechnologies(accountId, portfolioId, projectId));
	}

	@PutMapping("/{technologyCode}")
	public ResponseEntity<ProjectTechnologyResponse> updateTechnology(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId,
			@PathVariable UUID projectId,
			@PathVariable String technologyCode,
			@Valid @RequestBody ProjectTechnologyRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(technologyService.updateTechnology(
				accountId, portfolioId, projectId, technologyCode, request));
	}

	@DeleteMapping("/{technologyCode}")
	public ResponseEntity<Void> deleteTechnology(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId,
			@PathVariable UUID projectId,
			@PathVariable String technologyCode) {
		var accountId = UUID.fromString(jwt.getSubject());
		technologyService.deleteTechnology(accountId, portfolioId, projectId, technologyCode);
		return ResponseEntity.noContent().build();
	}
}
