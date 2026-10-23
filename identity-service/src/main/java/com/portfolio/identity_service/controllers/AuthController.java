package com.portfolio.identity_service.controllers;

import com.portfolio.identity_service.dto.request.LoginRequest;
import com.portfolio.identity_service.dto.request.RefreshRequest;
import com.portfolio.identity_service.dto.request.RegisterRequest;
import com.portfolio.identity_service.dto.response.TokenResponse;
import com.portfolio.identity_service.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	ResponseEntity<TokenResponse> register(
			@Valid @RequestBody RegisterRequest request,
			HttpServletRequest servletRequest) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(authService.register(request, servletRequest.getHeader("User-Agent"), clientIp(servletRequest)));
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
		return authService.login(request, servletRequest.getHeader("User-Agent"), clientIp(servletRequest));
	}

	@PostMapping("/refresh")
	TokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
		return authService.refresh(request.refreshToken(), servletRequest.getHeader("User-Agent"), clientIp(servletRequest));
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.noContent().build();
	}

	private static String clientIp(HttpServletRequest request) {
		return request.getRemoteAddr();
	}
}
