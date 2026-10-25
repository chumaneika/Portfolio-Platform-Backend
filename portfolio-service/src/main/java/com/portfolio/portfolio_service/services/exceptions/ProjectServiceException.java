package com.portfolio.portfolio_service.services.exceptions;

import java.util.Objects;

public class ProjectServiceException extends RuntimeException {

	private final Reason reason;

	private ProjectServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static ProjectServiceException portfolioNotFound() {
		return new ProjectServiceException(Reason.PORTFOLIO_NOT_FOUND, "Portfolio was not found");
	}

	public static ProjectServiceException projectNotFound() {
		return new ProjectServiceException(Reason.PROJECT_NOT_FOUND, "Project was not found");
	}

	public static ProjectServiceException slugAlreadyExists() {
		return new ProjectServiceException(Reason.SLUG_ALREADY_EXISTS, "Project slug already exists in this portfolio");
	}

	public static ProjectServiceException invalidTimeline(String message) {
		return new ProjectServiceException(Reason.INVALID_TIMELINE, message);
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		PORTFOLIO_NOT_FOUND,
		PROJECT_NOT_FOUND,
		SLUG_ALREADY_EXISTS,
		INVALID_TIMELINE
	}
}
