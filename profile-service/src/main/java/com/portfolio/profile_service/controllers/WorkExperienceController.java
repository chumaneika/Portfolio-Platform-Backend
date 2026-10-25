package com.portfolio.profile_service.controllers;

import com.portfolio.profile_service.dto.request.WorkExperienceRequest;
import com.portfolio.profile_service.dto.response.WorkExperienceResponse;
import com.portfolio.profile_service.services.WorkExperienceService;
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

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile/me/work-experiences")
@RequiredArgsConstructor
public class WorkExperienceController {

	private final WorkExperienceService workExperienceService;

	@PostMapping
	public ResponseEntity<WorkExperienceResponse> createWorkExperience(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody WorkExperienceRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		var response = workExperienceService.createWorkExperience(accountId, request);

		return ResponseEntity
				.created(URI.create("/api/v1/profile/me/work-experiences/" + response.id()))
				.body(response);
	}

	@GetMapping
	public ResponseEntity<List<WorkExperienceResponse>> getWorkExperiences(
			@AuthenticationPrincipal Jwt jwt) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(workExperienceService.getWorkExperiences(accountId));
	}

	@PutMapping("/{workExperienceId}")
	public ResponseEntity<WorkExperienceResponse> updateWorkExperience(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID workExperienceId,
			@Valid @RequestBody WorkExperienceRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(workExperienceService.updateWorkExperience(
				accountId,
				workExperienceId,
				request));
	}

	@DeleteMapping("/{workExperienceId}")
	public ResponseEntity<Void> deleteWorkExperience(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID workExperienceId) {
		var accountId = UUID.fromString(jwt.getSubject());
		workExperienceService.deleteWorkExperience(accountId, workExperienceId);
		return ResponseEntity.noContent().build();
	}

}
