package com.portfolio.profile_service.controllers;

import com.portfolio.profile_service.dto.request.CreateProfileSkillRequest;
import com.portfolio.profile_service.dto.request.ProfileSkillRequest;
import com.portfolio.profile_service.dto.response.ProfileSkillResponse;
import com.portfolio.profile_service.services.ProfileSkillService;
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
@RequestMapping("/api/v1/profile/me/skills")
@RequiredArgsConstructor
public class ProfileSkillController {

	private final ProfileSkillService profileSkillService;

	@PostMapping
	public ResponseEntity<ProfileSkillResponse> createSkill(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateProfileSkillRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		var response = profileSkillService.createSkill(accountId, request);
		var location = UriComponentsBuilder
				.fromPath("/api/v1/profile/me/skills")
				.pathSegment(response.skillCode())
				.build()
				.encode()
				.toUri();

		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public ResponseEntity<List<ProfileSkillResponse>> getSkills(
			@AuthenticationPrincipal Jwt jwt) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(profileSkillService.getSkills(accountId));
	}

	@PutMapping("/{skillCode}")
	public ResponseEntity<ProfileSkillResponse> updateSkill(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String skillCode,
			@Valid @RequestBody ProfileSkillRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(profileSkillService.updateSkill(accountId, skillCode, request));
	}

	@DeleteMapping("/{skillCode}")
	public ResponseEntity<Void> deleteSkill(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String skillCode) {
		var accountId = UUID.fromString(jwt.getSubject());
		profileSkillService.deleteSkill(accountId, skillCode);
		return ResponseEntity.noContent().build();
	}
}
