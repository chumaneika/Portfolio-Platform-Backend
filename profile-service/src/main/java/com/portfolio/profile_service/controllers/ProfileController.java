package com.portfolio.profile_service.controllers;

import com.portfolio.profile_service.dto.request.CreateProfileRequest;
import com.portfolio.profile_service.dto.request.ProfileRequest;
import com.portfolio.profile_service.dto.response.ProfileResponse;
import com.portfolio.profile_service.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileService profileService;

	@PutMapping("/me")
	public ResponseEntity<ProfileResponse> updateProfile(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ProfileRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		var profile = profileService.updateProfile(accountId, request);

		return ResponseEntity.ok(profile);
	}

	@PostMapping("/me")
	public ResponseEntity<Void> createProfile(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateProfileRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		profileService.createProfile(accountId, request.firstName(), request.lastName());

		return ResponseEntity.created(URI.create("/api/v1/profile/me")).build();
	}

	@GetMapping("/me")
	public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
		var accountId = UUID.fromString(jwt.getSubject());
		var profile = profileService.getProfile(accountId);

		return ResponseEntity.ok(profile);
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteProfile(@AuthenticationPrincipal Jwt jwt) {
		var accountId = UUID.fromString(jwt.getSubject());
		profileService.deleteProfile(accountId);
		return ResponseEntity.noContent().build();
	}
}
