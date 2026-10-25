package com.portfolio.profile_service;

import com.portfolio.profile_service.dto.request.ProfileRequest;
import com.portfolio.profile_service.repositories.ProfileRepository;
import com.portfolio.profile_service.services.ProfileService;
import com.portfolio.profile_service.services.exceptions.ProfileServiceException;
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
		"spring.datasource.url=jdbc:h2:mem:profile_service_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class ProfileEntityServiceTests {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private ProfileRepository profileRepository;

	@Test
	@Transactional
	@DisplayName("Profile creation uses the authenticated account and rejects duplicates")
	void createsProfileAndRejectsDuplicateAccount() {
		var accountId = UUID.randomUUID();

		var profileId = profileService.createProfile(accountId, "  Malik ", " Testov ");

		var storedProfile = profileRepository.findByAccountId(accountId).orElseThrow();
		assertThat(profileId).isEqualTo(storedProfile.getId());
		assertThat(storedProfile.getAccountId()).isEqualTo(accountId);
		assertThat(storedProfile.getFirstName()).isEqualTo("Malik");
		assertThat(storedProfile.getLastName()).isEqualTo("Testov");
		var response = profileService.getProfile(accountId);
		assertThat(response.id()).isEqualTo(profileId);
		assertThat(response.firstName()).isEqualTo("Malik");
		assertThat(response.lastName()).isEqualTo("Testov");

		assertThatThrownBy(() -> profileService.createProfile(accountId, "Another", "Name"))
				.isInstanceOfSatisfying(ProfileServiceException.class, exception ->
						assertThat(exception.getReason())
								.isEqualTo(ProfileServiceException.Reason.PROFILE_ALREADY_EXISTS))
				.hasMessage("A profile for this account already exists");
	}

	@Test
	@Transactional(readOnly = true)
	@DisplayName("Profile lookup reports when the authenticated account has no profile")
	void reportsMissingProfile() {
		assertThatThrownBy(() -> profileService.getProfile(UUID.randomUUID()))
				.isInstanceOfSatisfying(ProfileServiceException.class, exception ->
						assertThat(exception.getReason())
								.isEqualTo(ProfileServiceException.Reason.PROFILE_NOT_FOUND))
				.hasMessage("Profile was not found");
	}

	@Test
	@Transactional
	@DisplayName("Profile update changes all editable fields and returns a direct projection")
	void updatesProfile() {
		var accountId = UUID.randomUUID();
		var profileId = profileService.createProfile(accountId, "Old", "Name");
		var request = new ProfileRequest(
				"  Malik ",
				" Testov ",
				" Java Developer ",
				" Backend developer ",
				" Moscow ",
				" https://example.com/avatar.png ",
				" https://example.com/resume.pdf ",
				true);

		var response = profileService.updateProfile(accountId, request);

		assertThat(response.id()).isEqualTo(profileId);
		assertThat(response.firstName()).isEqualTo("Malik");
		assertThat(response.lastName()).isEqualTo("Testov");
		assertThat(response.headline()).isEqualTo("Java Developer");
		assertThat(response.bio()).isEqualTo("Backend developer");
		assertThat(response.location()).isEqualTo("Moscow");
		assertThat(response.avatarUrl()).isEqualTo("https://example.com/avatar.png");
		assertThat(response.resumeUrl()).isEqualTo("https://example.com/resume.pdf");
		assertThat(response.availableForWork()).isTrue();
	}

	@Test
	@Transactional
	@DisplayName("Profile can be deleted by its authenticated account")
	void deletesProfile() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");

		profileService.deleteProfile(accountId);

		assertThat(profileRepository.existsByAccountId(accountId)).isFalse();
		assertThatThrownBy(() -> profileService.getProfile(accountId))
				.isInstanceOfSatisfying(ProfileServiceException.class, exception ->
						assertThat(exception.getReason())
								.isEqualTo(ProfileServiceException.Reason.PROFILE_NOT_FOUND));
	}
}
