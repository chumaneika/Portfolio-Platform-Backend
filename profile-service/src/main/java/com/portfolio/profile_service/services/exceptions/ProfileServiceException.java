package com.portfolio.profile_service.services.exceptions;

import java.util.Objects;

public class ProfileServiceException extends RuntimeException {

	private final Reason reason;

	public ProfileServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static ProfileServiceException profileAlreadyExists() {
		return new ProfileServiceException(
				Reason.PROFILE_ALREADY_EXISTS,
				"A profile for this account already exists");
	}

	public static ProfileServiceException profileNotFound() {
		return new ProfileServiceException(
				Reason.PROFILE_NOT_FOUND,
				"Profile was not found");
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		PROFILE_ALREADY_EXISTS,
		PROFILE_NOT_FOUND
	}
}
