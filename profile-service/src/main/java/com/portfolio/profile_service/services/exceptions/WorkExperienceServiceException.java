package com.portfolio.profile_service.services.exceptions;

import java.util.Objects;

public class WorkExperienceServiceException extends RuntimeException {

	private final Reason reason;

	public WorkExperienceServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static WorkExperienceServiceException profileNotFound() {
		return new WorkExperienceServiceException(
				Reason.PROFILE_NOT_FOUND,
				"Profile was not found");
	}

	public static WorkExperienceServiceException workExperienceNotFound() {
		return new WorkExperienceServiceException(
				Reason.WORK_EXPERIENCE_NOT_FOUND,
				"Work experience was not found");
	}

	public static WorkExperienceServiceException invalidPeriod(String message) {
		return new WorkExperienceServiceException(Reason.INVALID_PERIOD, message);
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		PROFILE_NOT_FOUND,
		WORK_EXPERIENCE_NOT_FOUND,
		INVALID_PERIOD
	}
}
