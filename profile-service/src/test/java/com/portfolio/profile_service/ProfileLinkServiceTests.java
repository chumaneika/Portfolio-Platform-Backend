package com.portfolio.profile_service;

import com.portfolio.profile_service.dto.request.ProfileLinkRequest;
import com.portfolio.profile_service.services.ProfileLinkService;
import com.portfolio.profile_service.services.ProfileService;
import com.portfolio.profile_service.services.exceptions.ProfileLinkServiceException;
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
		"spring.datasource.url=jdbc:h2:mem:profile_link_service_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class ProfileLinkServiceTests {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private ProfileLinkService profileLinkService;

	@Test
	@Transactional
	@DisplayName("Profile link can be created, listed and updated by its profile owner")
	void createListAndUpdateLink() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");

		var created = profileLinkService.createLink(
				accountId,
				new ProfileLinkRequest(" GitHub ", "Source code", "https://github.com/user"));

		assertThat(created.platform()).isEqualTo("github");
		assertThat(created.displayOrder()).isZero();
		assertThat(profileLinkService.getLinks(accountId))
				.extracting(response -> response.id())
				.containsExactly(created.id());

		var updated = profileLinkService.updateLink(
				accountId,
				created.id(),
				new ProfileLinkRequest("LinkedIn", "Professional profile", "https://linkedin.com/in/user"));

		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.platform()).isEqualTo("linkedin");
		assertThat(updated.label()).isEqualTo("Professional profile");
		assertThat(updated.url()).isEqualTo("https://linkedin.com/in/user");
	}

	@Test
	@Transactional
	@DisplayName("Profile link ownership is enforced and order is compacted after deletion")
	void enforcesOwnershipAndCompactsOrder() {
		var ownerAccountId = UUID.randomUUID();
		var otherAccountId = UUID.randomUUID();
		profileService.createProfile(ownerAccountId, "Owner", "User");
		profileService.createProfile(otherAccountId, "Other", "User");
		var first = profileLinkService.createLink(
				ownerAccountId,
				new ProfileLinkRequest("github", null, "https://github.com/user"));
		var second = profileLinkService.createLink(
				ownerAccountId,
				new ProfileLinkRequest("linkedin", null, "https://linkedin.com/in/user"));

		assertThatThrownBy(() -> profileLinkService.deleteLink(otherAccountId, first.id()))
				.isInstanceOfSatisfying(ProfileLinkServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								ProfileLinkServiceException.Reason.LINK_NOT_FOUND));

		profileLinkService.deleteLink(ownerAccountId, first.id());
		var remaining = profileLinkService.getLinks(ownerAccountId);
		assertThat(remaining).hasSize(1);
		assertThat(remaining.getFirst().id()).isEqualTo(second.id());
		assertThat(remaining.getFirst().displayOrder()).isZero();
	}

	@Test
	@Transactional
	@DisplayName("Profile link rejects non-HTTP URLs")
	void rejectsInvalidUrl() {
		var accountId = UUID.randomUUID();
		profileService.createProfile(accountId, "Malik", "Testov");

		assertThatThrownBy(() -> profileLinkService.createLink(
				accountId,
				new ProfileLinkRequest("github", null, "github.com/user")))
				.isInstanceOfSatisfying(ProfileLinkServiceException.class, exception ->
						assertThat(exception.getReason()).isEqualTo(
								ProfileLinkServiceException.Reason.INVALID_URL));
	}
}
