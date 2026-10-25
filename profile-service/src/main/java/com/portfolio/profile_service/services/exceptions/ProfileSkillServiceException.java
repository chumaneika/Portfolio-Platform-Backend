package com.portfolio.profile_service.services.exceptions;

import java.util.Objects;

public class ProfileSkillServiceException extends RuntimeException {

	private final Reason reason;

	public ProfileSkillServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static ProfileSkillServiceException profileNotFound() {
		return new ProfileSkillServiceException(
				Reason.PROFILE_NOT_FOUND,
				"Profile was not found");
	}

	public static ProfileSkillServiceException skillNotFound() {
		return new ProfileSkillServiceException(
				Reason.SKILL_NOT_FOUND,
				"Profile skill was not found");
	}

	public static ProfileSkillServiceException skillAlreadyExists() {
		return new ProfileSkillServiceException(
				Reason.SKILL_ALREADY_EXISTS,
				"This skill is already present in the profile");
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		PROFILE_NOT_FOUND,
		SKILL_NOT_FOUND,
		SKILL_ALREADY_EXISTS
	}
}
