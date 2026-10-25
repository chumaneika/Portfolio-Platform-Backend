package com.portfolio.profile_service;

import com.portfolio.profile_service.dto.request.EducationRequest;
import com.portfolio.profile_service.services.EducationService;
import com.portfolio.profile_service.services.ProfileService;
import com.portfolio.profile_service.services.exceptions.EducationServiceException;
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
		"spring.datasource.url=jdbc:h2:mem:education_service_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class EducationServiceTests {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private EducationService educationService;

	@Test
	@Transactional
	@DisplayName("Education can be created, listed and updated by its profile owner")
	void createListAndUpdateEducation() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");

		var created = educationService.createEducation(
				accountId,
				request("University", true, null));

		assertThat(created.institutionName()).isEqualTo("University");
		assertThat(created.currentEducation()).isTrue();
		assertThat(created.endedOn()).isNull();
		assertThat(created.displayOrder()).isZero();
		assertThat(educationService.getEducations(accountId))
				.extracting(response -> response.id())
				.containsExactly(created.id());

		var updated = educationService.updateEducation(
				accountId,
				created.id(),
				request("New University", false, LocalDate.of(2025, 6, 1)));

		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.institutionName()).isEqualTo("New University");
		assertThat(updated.degree()).isEqualTo("Bachelor");
		assertThat(updated.fieldOfStudy()).isEqualTo("Computer Science");
		assertThat(updated.currentEducation()).isFalse();
		assertThat(updated.endedOn()).isEqualTo(LocalDate.of(2025, 6, 1));
	}

	@Test
	@Transactional
	@DisplayName("Education order is assigned by the service")
	void assignsDisplayOrder() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");

		var first = educationService.createEducation(accountId, request("First", true, null));
		var second = educationService.createEducation(accountId, request("Second", true, null));

		assertThat(first.displayOrder()).isZero();
		assertThat(second.displayOrder()).isEqualTo(1);
	}

	@Test
	@Transactional
	@DisplayName("Education validates dates and ownership")
	void validatesDatesAndOwnership() {
		var ownerAccountId = UUID.randomUUID();
		var otherAccountId = UUID.randomUUID();
		profileService.createProfile(ownerAccountId, "Owner", "User");
		profileService.createProfile(otherAccountId, "Other", "User");
		var created = educationService.createEducation(
				ownerAccountId,
				request("University", true, null));

		assertThatThrownBy(() -> educationService.updateEducation(
				otherAccountId,
				created.id(),
				request("University", true, null)))
				.isInstanceOfSatisfying(EducationServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								EducationServiceException.Reason.EDUCATION_NOT_FOUND));

		assertThatThrownBy(() -> educationService.createEducation(
				ownerAccountId,
				new EducationRequest(
						"University",
						"Bachelor",
						"Computer Science",
						"Description",
						LocalDate.of(2025, 1, 1),
						LocalDate.of(2024, 1, 1),
						false)))
				.isInstanceOfSatisfying(EducationServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								EducationServiceException.Reason.INVALID_PERIOD));
	}

	@Test
	@Transactional
	@DisplayName("Education can be deleted only by its owner and order is compacted")
	void deletesOwnedEducationAndCompactsOrder() {
		var ownerAccountId = UUID.randomUUID();
		var otherAccountId = UUID.randomUUID();
		profileService.createProfile(ownerAccountId, "Owner", "User");
		profileService.createProfile(otherAccountId, "Other", "User");
		var first = educationService.createEducation(
				ownerAccountId,
				request("First", true, null));
		var second = educationService.createEducation(
				ownerAccountId,
				request("Second", true, null));

		assertThatThrownBy(() -> educationService.deleteEducation(otherAccountId, first.id()))
				.isInstanceOfSatisfying(EducationServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								EducationServiceException.Reason.EDUCATION_NOT_FOUND));

		educationService.deleteEducation(ownerAccountId, first.id());
		var remaining = educationService.getEducations(ownerAccountId);
		assertThat(remaining).hasSize(1);
		assertThat(remaining.getFirst().id()).isEqualTo(second.id());
		assertThat(remaining.getFirst().displayOrder()).isZero();
	}

	private static EducationRequest request(
			String institutionName,
			boolean currentEducation,
			LocalDate endedOn) {
		return new EducationRequest(
				institutionName,
				"Bachelor",
				"Computer Science",
				"Description",
				LocalDate.of(2024, 1, 1),
				endedOn,
				currentEducation);
	}
}
