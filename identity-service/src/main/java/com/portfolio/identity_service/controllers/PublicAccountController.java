package com.portfolio.identity_service.controllers;

import com.portfolio.identity_service.dto.response.PublicAccountResponse;
import com.portfolio.identity_service.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class PublicAccountController {

	private final AccountService accountService;

	@GetMapping("/{accountId}/public")
	public ResponseEntity<PublicAccountResponse> getPublicAccount(@PathVariable UUID accountId) {
		return ResponseEntity.ok(accountService.getPublicAccount(accountId));
	}
}
