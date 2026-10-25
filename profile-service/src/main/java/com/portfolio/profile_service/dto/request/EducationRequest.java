package com.portfolio.profile_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EducationRequest(
		@NotBlank(message = "Institution name must not be blank")
		@Size(max = 200, message = "Institution name must not exceed 200 characters")
		String institutionName,

		@Size(max = 150, message = "Degree must not exceed 150 characters")
		String degree,

		@Size(max = 150, message = "Field of study must not exceed 150 characters")
		String fieldOfStudy,

		@Size(max = 10000, message = "Description must not exceed 10000 characters")
		String description,

		LocalDate startedOn,

		LocalDate endedOn,

		@NotNull(message = "Current education must be specified")
		Boolean currentEducation) {
}
