package com.portfolio.profile_service.dto.request;

import com.portfolio.profile_service.entities.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WorkExperienceRequest(
		@NotBlank(message = "Company name must not be blank")
		@Size(max = 150, message = "Company name must not exceed 150 characters")
		String companyName,

		@NotBlank(message = "Position title must not be blank")
		@Size(max = 150, message = "Position title must not exceed 150 characters")
		String positionTitle,

		EmploymentType employmentType,

		@Size(max = 200, message = "Location must not exceed 200 characters")
		String location,

		@Size(max = 10000, message = "Description must not exceed 10000 characters")
		String description,

		@NotNull(message = "Start date must be specified")
		LocalDate startedOn,

		LocalDate endedOn,

		@NotNull(message = "Current position must be specified")
		Boolean currentPosition) {
}
