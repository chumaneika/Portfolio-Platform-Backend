package com.portfolio.portfolio_service.services.exceptions;

import com.portfolio.portfolio_service.entites.PortfolioStatus;

import java.util.Objects;

public class PortfolioServiceException extends RuntimeException {

	private final Reason reason;

	private PortfolioServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static PortfolioServiceException portfolioNotFound() {
		return new PortfolioServiceException(Reason.PORTFOLIO_NOT_FOUND, "Portfolio was not found");
	}

	public static PortfolioServiceException slugAlreadyExists() {
		return new PortfolioServiceException(Reason.SLUG_ALREADY_EXISTS, "Portfolio slug already exists");
	}

	public static PortfolioServiceException invalidSlug() {
		return new PortfolioServiceException(Reason.INVALID_SLUG, "Portfolio slug 'me' is reserved");
	}

	public static PortfolioServiceException invalidStatusTransition(PortfolioStatus current, PortfolioStatus target) {
		return new PortfolioServiceException(Reason.INVALID_STATUS_TRANSITION,
				"Cannot change portfolio status from " + current + " to " + target);
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		PORTFOLIO_NOT_FOUND,
		SLUG_ALREADY_EXISTS,
		INVALID_SLUG,
		INVALID_STATUS_TRANSITION
	}
}
