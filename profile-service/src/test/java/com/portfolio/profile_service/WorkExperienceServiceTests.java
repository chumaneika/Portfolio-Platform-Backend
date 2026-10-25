package com.portfolio.profile_service;

import com.portfolio.profile_service.dto.request.WorkExperienceRequest;
import com.portfolio.profile_service.entities.EmploymentType;
import com.portfolio.profile_service.services.ProfileService;
import com.portfolio.profile_service.services.WorkExperienceService;
import com.portfolio.profile_service.services.exceptions.WorkExperienceServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false",
		"spring.datasource.url=jdbc:h2:mem:work_experience_service_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class WorkExperienceServiceTests {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private WorkExperienceService workExperienceService;

	@Test
	@Transactional
	@DisplayName("Work experience can be created, listed and updated by its profile owner")
	void createListAndUpdateWorkExperience() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");

		var created = workExperienceService.createWorkExperience(
				accountId,
				request("Company", "Developer", true, null));

		assertThat(created.companyName()).isEqualTo("Company");
		assertThat(created.currentPosition()).isTrue();
		assertThat(created.endedOn()).isNull();
		assertThat(created.displayOrder()).isZero();
		assertThat(workExperienceService.getWorkExperiences(accountId))
				.extracting(response -> response.id())
				.containsExactly(created.id());

		var updated = workExperienceService.updateWorkExperience(
				accountId,
				created.id(),
				request("New Company", "Senior Developer", false, LocalDate.of(2025, 1, 1)));

		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.companyName()).isEqualTo("New Company");
		assertThat(updated.positionTitle()).isEqualTo("Senior Developer");
		assertThat(updated.currentPosition()).isFalse();
		assertThat(updated.endedOn()).isEqualTo(LocalDate.of(2025, 1, 1));
	}

	@Test
	@Transactional
	@DisplayName("Work experience order is assigned by the service")
	void assignsDisplayOrder() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");

		var first = workExperienceService.createWorkExperience(
				accountId,
				request("First", "Developer", true, null));
		var second = workExperienceService.createWorkExperience(
				accountId,
				request("Second", "Developer", true, null));

		assertThat(first.displayOrder()).isZero();
		assertThat(second.displayOrder()).isEqualTo(1);
	}

	@Test
	@Transactional
	@DisplayName("Work experience validates dates and ownership")
	void validatesDatesAndOwnership() {
		var ownerAccountId = UUID.randomUUID();
		var otherAccountId = UUID.randomUUID();
		profileService.createProfile(ownerAccountId, "Owner", "User");
		profileService.createProfile(otherAccountId, "Other", "User");
		var created = workExperienceService.createWorkExperience(
				ownerAccountId,
				request("Company", "Developer", true, null));

		assertThatThrownBy(() -> workExperienceService.updateWorkExperience(
				otherAccountId,
				created.id(),
				request("Company", "Developer", true, null)))
				.isInstanceOfSatisfying(WorkExperienceServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								WorkExperienceServiceException.Reason.WORK_EXPERIENCE_NOT_FOUND));

		assertThatThrownBy(() -> workExperienceService.createWorkExperience(
				ownerAccountId,
				request("Company", "Developer", true, LocalDate.of(2025, 1, 1))))
				.isInstanceOfSatisfying(WorkExperienceServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								WorkExperienceServiceException.Reason.INVALID_PERIOD));
	}

	@Test
	@Transactional
	@DisplayName("Work experience can be deleted only by its owner and order is compacted")
	void deletesOwnedWorkExperienceAndCompactsOrder() {
		var ownerAccountId = UUID.randomUUID();
		var otherAccountId = UUID.randomUUID();
		profileService.createProfile(ownerAccountId, "Owner", "User");
		profileService.createProfile(otherAccountId, "Other", "User");
		var first = workExperienceService.createWorkExperience(
				ownerAccountId,
				request("First", "Developer", true, null));
		var second = workExperienceService.createWorkExperience(
				ownerAccountId,
				request("Second", "Developer", true, null));

		assertThatThrownBy(() -> workExperienceService.deleteWorkExperience(
				otherAccountId,
				first.id()))
				.isInstanceOfSatisfying(WorkExperienceServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								WorkExperienceServiceException.Reason.WORK_EXPERIENCE_NOT_FOUND));

		workExperienceService.deleteWorkExperience(ownerAccountId, first.id());
		var remaining = workExperienceService.getWorkExperiences(ownerAccountId);
		assertThat(remaining).hasSize(1);
		assertThat(remaining.getFirst().id()).isEqualTo(second.id());
		assertThat(remaining.getFirst().displayOrder()).isZero();
	}

	private static WorkExperienceRequest request(
			String companyName,
			String positionTitle,
			boolean currentPosition,
			LocalDate endedOn) {
		return new WorkExperienceRequest(
				companyName,
				positionTitle,
				EmploymentType.FULL_TIME,
				"Moscow",
				"Backend development",
				LocalDate.of(2024, 1, 1),
				endedOn,
				currentPosition);
	}
}
