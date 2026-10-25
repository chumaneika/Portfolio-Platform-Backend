package com.portfolio.profile_service.controllers;

import com.portfolio.profile_service.dto.request.EducationRequest;
import com.portfolio.profile_service.dto.response.EducationResponse;
import com.portfolio.profile_service.services.EducationService;
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
@RequestMapping("/api/v1/profile/me/educations")
@RequiredArgsConstructor
public class EducationController {

	private final EducationService educationService;

	@PostMapping
	public ResponseEntity<EducationResponse> createEducation(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody EducationRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		var response = educationService.createEducation(accountId, request);

		return ResponseEntity
				.created(URI.create("/api/v1/profile/me/educations/" + response.id()))
				.body(response);
	}

	@GetMapping
	public ResponseEntity<List<EducationResponse>> getEducations(
			@AuthenticationPrincipal Jwt jwt) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(educationService.getEducations(accountId));
	}

	@PutMapping("/{educationId}")
	public ResponseEntity<EducationResponse> updateEducation(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID educationId,
			@Valid @RequestBody EducationRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(educationService.updateEducation(
				accountId,
				educationId,
				request));
	}

	@DeleteMapping("/{educationId}")
	public ResponseEntity<Void> deleteEducation(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID educationId) {
		var accountId = UUID.fromString(jwt.getSubject());
		educationService.deleteEducation(accountId, educationId);
		return ResponseEntity.noContent().build();
	}
}
