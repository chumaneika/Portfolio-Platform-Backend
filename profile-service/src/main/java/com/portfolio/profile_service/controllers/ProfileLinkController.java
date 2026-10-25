package com.portfolio.profile_service.controllers;

import com.portfolio.profile_service.dto.request.ProfileLinkRequest;
import com.portfolio.profile_service.dto.response.ProfileLinkResponse;
import com.portfolio.profile_service.services.ProfileLinkService;
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
@RequestMapping("/api/v1/profile/me/links")
@RequiredArgsConstructor
public class ProfileLinkController {

	private final ProfileLinkService profileLinkService;

	@PostMapping
	public ResponseEntity<ProfileLinkResponse> createLink(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ProfileLinkRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		var response = profileLinkService.createLink(accountId, request);
		var location = UriComponentsBuilder
				.fromPath("/api/v1/profile/me/links")
				.pathSegment(response.id().toString())
				.build()
				.toUri();

		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public ResponseEntity<List<ProfileLinkResponse>> getLinks(
			@AuthenticationPrincipal Jwt jwt) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(profileLinkService.getLinks(accountId));
	}

	@PutMapping("/{linkId}")
	public ResponseEntity<ProfileLinkResponse> updateLink(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID linkId,
			@Valid @RequestBody ProfileLinkRequest request) {
		var accountId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(profileLinkService.updateLink(accountId, linkId, request));
	}

	@DeleteMapping("/{linkId}")
	public ResponseEntity<Void> deleteLink(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID linkId) {
		var accountId = UUID.fromString(jwt.getSubject());
		profileLinkService.deleteLink(accountId, linkId);
		return ResponseEntity.noContent().build();
	}
}
