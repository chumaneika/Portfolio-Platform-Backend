package com.portfolio.identity_service.services.exceptions;

import java.util.Objects;

public class AccountServiceException extends RuntimeException {

	private final Reason reason;

	private AccountServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static AccountServiceException accountNotFound() {
		return new AccountServiceException(Reason.ACCOUNT_NOT_FOUND, "Account was not found");
	}

	public static AccountServiceException accountInactive() {
		return new AccountServiceException(Reason.ACCOUNT_INACTIVE, "Account is not active");
	}

	public static AccountServiceException invalidCurrentPassword() {
		return new AccountServiceException(Reason.INVALID_CURRENT_PASSWORD, "Current password is incorrect");
	}

	public static AccountServiceException passwordUnchanged() {
		return new AccountServiceException(Reason.PASSWORD_UNCHANGED, "New password must differ from current password");
	}

	public static AccountServiceException passwordTooLong() {
		return new AccountServiceException(Reason.PASSWORD_TOO_LONG, "Password must not exceed 72 UTF-8 bytes");
	}

	public static AccountServiceException usernameAlreadyExists() {
		return new AccountServiceException(Reason.USERNAME_ALREADY_EXISTS, "Username is already in use");
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		ACCOUNT_NOT_FOUND,
		ACCOUNT_INACTIVE,
		INVALID_CURRENT_PASSWORD,
		PASSWORD_UNCHANGED,
		PASSWORD_TOO_LONG,
		USERNAME_ALREADY_EXISTS
	}
}
