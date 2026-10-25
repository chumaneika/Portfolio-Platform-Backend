package com.portfolio.profile_service.services.exceptions;

import java.util.Objects;

public class EducationServiceException extends RuntimeException {

	private final Reason reason;

	public EducationServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static EducationServiceException profileNotFound() {
		return new EducationServiceException(
				Reason.PROFILE_NOT_FOUND,
				"Profile was not found");
	}

	public static EducationServiceException educationNotFound() {
		return new EducationServiceException(
				Reason.EDUCATION_NOT_FOUND,
				"Education was not found");
	}

	public static EducationServiceException invalidPeriod(String message) {
		return new EducationServiceException(Reason.INVALID_PERIOD, message);
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		PROFILE_NOT_FOUND,
		EDUCATION_NOT_FOUND,
		INVALID_PERIOD
	}
}
