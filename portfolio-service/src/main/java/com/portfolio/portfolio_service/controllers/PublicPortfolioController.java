package com.portfolio.portfolio_service.controllers;

import com.portfolio.portfolio_service.dto.response.PublicPortfolioResponse;
import com.portfolio.portfolio_service.services.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PublicPortfolioController {

	private final PortfolioService portfolioService;

	@GetMapping("/{slug}")
	public ResponseEntity<PublicPortfolioResponse> getPublishedPortfolio(@PathVariable String slug) {
		return ResponseEntity.ok(portfolioService.getPublishedPortfolio(slug));
	}
}
