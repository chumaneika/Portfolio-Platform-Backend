package com.portfolio.profile_service.controllers;

import com.portfolio.profile_service.dto.response.PublicProfileResponse;
import com.portfolio.profile_service.services.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class PublicProfileController {

	private final PublicProfileService publicProfileService;

	@GetMapping("/by-account/{accountId}")
	public ResponseEntity<PublicProfileResponse> getPublicProfileByAccountId(
			@PathVariable UUID accountId) {
		return ResponseEntity.ok(publicProfileService.getPublicProfileByAccountId(accountId));
	}

	@GetMapping("/{profileId}")
	public ResponseEntity<PublicProfileResponse> getPublicProfile(
			@PathVariable UUID profileId) {
		return ResponseEntity.ok(publicProfileService.getPublicProfile(profileId));
	}
}
