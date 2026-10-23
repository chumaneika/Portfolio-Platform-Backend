package com.portfolio.profile_service.controllers;

import com.portfolio.profile_service.dto.ApiError;
import com.portfolio.profile_service.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApiException(
			ApiException exception,
			HttpServletRequest request) {
		return response(
				exception.getStatus(),
				exception.getCode(),
				exception.getMessage(),
				request,
				Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleBodyValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		var fields = new LinkedHashMap<String, String>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return response(
				HttpStatus.BAD_REQUEST,
				"validation_failed",
				"Request validation failed",
				request,
				fields);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiError> handleMethodValidation(
			HandlerMethodValidationException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"validation_failed",
				"Request validation failed",
				request,
				Map.of());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleMalformedJson(
			HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"malformed_json",
				"Request body is missing or malformed",
				request,
				Map.of());
	}

	@ExceptionHandler({
			MethodArgumentTypeMismatchException.class,
			MissingRequestValueException.class
	})
	public ResponseEntity<ApiError> handleInvalidRequest(
			Exception exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"invalid_request",
				"Request is invalid",
				request,
				Map.of());
	}

	@ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
	public ResponseEntity<ApiError> handleNotFound(
			Exception exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.NOT_FOUND,
				"resource_not_found",
				"Resource not found",
				request,
				Map.of());
	}

	@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> handleMethodNotAllowed(
			org.springframework.web.HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.METHOD_NOT_ALLOWED,
				"method_not_allowed",
				"HTTP method is not supported for this resource",
				request,
				Map.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(
			Exception exception,
			HttpServletRequest request) {
		log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
		return response(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"internal_error",
				"An unexpected error occurred",
				request,
				Map.of());
	}

	private ResponseEntity<ApiError> response(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request,
			Map<String, String> fieldErrors) {
		return ResponseEntity.status(status).body(new ApiError(
				OffsetDateTime.now(ZoneOffset.UTC),
				status.value(),
				status.getReasonPhrase(),
				code,
				message,
				request.getRequestURI(),
				fieldErrors));
	}
}
