package com.portfolio.portfolio_service.controllers;

import com.portfolio.portfolio_service.dto.request.ProjectRequest;
import com.portfolio.portfolio_service.dto.response.ProjectResponse;
import com.portfolio.portfolio_service.services.ProjectService;
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
@RequestMapping("/api/v1/portfolios/me/{portfolioId}/projects")
@RequiredArgsConstructor
public class ProjectController {

	private final ProjectService projectService;

	@PostMapping
	public ResponseEntity<ProjectResponse> createProject(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId,
			@Valid @RequestBody ProjectRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		var response = projectService.createProject(accountId, portfolioId, request);
		var location = UriComponentsBuilder.fromPath("/api/v1/portfolios/me")
				.pathSegment(portfolioId.toString(), "projects", response.id().toString())
				.build()
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public ResponseEntity<List<ProjectResponse>> getProjects(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(projectService.getProjects(accountId, portfolioId));
	}

	@PutMapping("/{projectId}")
	public ResponseEntity<ProjectResponse> updateProject(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId,
			@PathVariable UUID projectId,
			@Valid @RequestBody ProjectRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(projectService.updateProject(accountId, portfolioId, projectId, request));
	}

	@DeleteMapping("/{projectId}")
	public ResponseEntity<Void> deleteProject(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId,
			@PathVariable UUID projectId) {
		var accountId = UUID.fromString(jwt.getSubject());
		projectService.deleteProject(accountId, portfolioId, projectId);
		return ResponseEntity.noContent().build();
	}
}
