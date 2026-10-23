package com.portfolio.identity_service.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.code = Objects.requireNonNull(code, "code must not be null");
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
