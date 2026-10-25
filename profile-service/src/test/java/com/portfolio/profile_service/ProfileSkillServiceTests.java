package com.portfolio.profile_service;

import com.portfolio.profile_service.dto.request.CreateProfileSkillRequest;
import com.portfolio.profile_service.dto.request.ProfileSkillRequest;
import com.portfolio.profile_service.services.ProfileService;
import com.portfolio.profile_service.services.ProfileSkillService;
import com.portfolio.profile_service.services.exceptions.ProfileSkillServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false",
		"spring.datasource.url=jdbc:h2:mem:profile_skill_service_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class ProfileSkillServiceTests {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private ProfileSkillService profileSkillService;

	@Test
	@Transactional
	@DisplayName("Profile skill can be created, listed and updated by its profile owner")
	void createListAndUpdateSkill() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");

		var created = profileSkillService.createSkill(
				accountId,
				new CreateProfileSkillRequest(" Java ", "Java", "Backend"));

		assertThat(created.skillCode()).isEqualTo("java");
		assertThat(created.displayOrder()).isZero();
		assertThat(profileSkillService.getSkills(accountId))
				.extracting(response -> response.skillCode())
				.containsExactly("java");

		var updated = profileSkillService.updateSkill(
				accountId,
				"JAVA",
				new ProfileSkillRequest("Java 21", "Languages"));

		assertThat(updated.skillCode()).isEqualTo("java");
		assertThat(updated.displayName()).isEqualTo("Java 21");
		assertThat(updated.category()).isEqualTo("Languages");
	}

	@Test
	@Transactional
	@DisplayName("Profile rejects duplicate normalized skill codes")
	void rejectsDuplicateSkill() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");
		profileSkillService.createSkill(
				accountId,
				new CreateProfileSkillRequest("java", "Java", "Backend"));

		assertThatThrownBy(() -> profileSkillService.createSkill(
				accountId,
				new CreateProfileSkillRequest(" JAVA ", "Java", "Backend")))
				.isInstanceOfSatisfying(ProfileSkillServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								ProfileSkillServiceException.Reason.SKILL_ALREADY_EXISTS));
	}

	@Test
	@Transactional
	@DisplayName("Profile skill ownership is enforced and order is compacted after deletion")
	void enforcesOwnershipAndCompactsOrder() {
		var ownerAccountId = UUID.randomUUID();
		var otherAccountId = UUID.randomUUID();
		profileService.createProfile(ownerAccountId, "Owner", "User");
		profileService.createProfile(otherAccountId, "Other", "User");
		profileSkillService.createSkill(
				ownerAccountId,
				new CreateProfileSkillRequest("java", "Java", "Backend"));
		profileSkillService.createSkill(
				ownerAccountId,
				new CreateProfileSkillRequest("spring", "Spring", "Backend"));

		assertThatThrownBy(() -> profileSkillService.updateSkill(
				otherAccountId,
				"java",
				new ProfileSkillRequest("Changed", "Backend")))
				.isInstanceOfSatisfying(ProfileSkillServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								ProfileSkillServiceException.Reason.SKILL_NOT_FOUND));

		profileSkillService.deleteSkill(ownerAccountId, "java");
		var remaining = profileSkillService.getSkills(ownerAccountId);
		assertThat(remaining).hasSize(1);
		assertThat(remaining.getFirst().skillCode()).isEqualTo("spring");
		assertThat(remaining.getFirst().displayOrder()).isZero();
	}
}
