package com.portfolio.identity_service.controllers;

import com.portfolio.identity_service.dto.request.ChangePasswordRequest;
import com.portfolio.identity_service.dto.request.ChangeUsernameRequest;
import com.portfolio.identity_service.dto.response.AccountSummaryResponse;
import com.portfolio.identity_service.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/me")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@GetMapping
	public ResponseEntity<AccountSummaryResponse> getAccount(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(accountService.getAccount(UUID.fromString(jwt.getSubject())));
	}

	@PutMapping("/password")
	public ResponseEntity<Void> changePassword(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ChangePasswordRequest request) {
		accountService.changePassword(UUID.fromString(jwt.getSubject()), request);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/sessions")
	public ResponseEntity<Void> revokeSessions(@AuthenticationPrincipal Jwt jwt) {
		accountService.revokeSessions(UUID.fromString(jwt.getSubject()));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/username")
	public ResponseEntity<Void> changeUsername(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ChangeUsernameRequest request) {
		accountService.changeUsername(UUID.fromString(jwt.getSubject()), request);
		return ResponseEntity.noContent().build();
	}
}
