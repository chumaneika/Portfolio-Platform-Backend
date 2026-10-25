package com.portfolio.profile_service.services.exceptions;

import java.util.Objects;

public class ProfileLinkServiceException extends RuntimeException {

	private final Reason reason;

	public ProfileLinkServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static ProfileLinkServiceException profileNotFound() {
		return new ProfileLinkServiceException(
				Reason.PROFILE_NOT_FOUND,
				"Profile was not found");
	}

	public static ProfileLinkServiceException linkNotFound() {
		return new ProfileLinkServiceException(
				Reason.LINK_NOT_FOUND,
				"Profile link was not found");
	}

	public static ProfileLinkServiceException invalidUrl() {
		return new ProfileLinkServiceException(
				Reason.INVALID_URL,
				"URL must be an absolute HTTP or HTTPS URL");
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		PROFILE_NOT_FOUND,
		LINK_NOT_FOUND,
		INVALID_URL
	}
}
