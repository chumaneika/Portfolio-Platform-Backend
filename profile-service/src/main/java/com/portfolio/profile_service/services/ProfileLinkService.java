package com.portfolio.profile_service.services;

import com.portfolio.profile_service.dto.request.ProfileLinkRequest;
import com.portfolio.profile_service.dto.response.ProfileLinkResponse;
import com.portfolio.profile_service.entities.ProfileLinkEntity;
import com.portfolio.profile_service.repositories.ProfileLinkRepository;
import com.portfolio.profile_service.repositories.ProfileRepository;
import com.portfolio.profile_service.services.exceptions.ProfileLinkServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileLinkService {

	private final ProfileRepository profileRepository;
	private final ProfileLinkRepository profileLinkRepository;

	@Transactional
	public ProfileLinkResponse createLink(UUID accountId, ProfileLinkRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var profile = profileRepository.findByAccountId(accountId)
				.orElseThrow(ProfileLinkServiceException::profileNotFound);
		var profileLink = new ProfileLinkEntity(
				profile,
				request.platform(),
				request.label(),
				validateUrl(request.url()),
				profileLinkRepository.nextDisplayOrder(profile.getId()));

		profileLinkRepository.save(profileLink);
		return findResponse(profileLink.getId(), accountId);
	}

	@Transactional(readOnly = true)
	public List<ProfileLinkResponse> getLinks(UUID accountId) {
		if (!profileRepository.existsByAccountId(accountId)) {
			throw ProfileLinkServiceException.profileNotFound();
		}
		return profileLinkRepository.findAllResponsesByAccountId(accountId);
	}

	@Transactional
	public ProfileLinkResponse updateLink(
			UUID accountId,
			UUID profileLinkId,
			ProfileLinkRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (!profileRepository.existsByAccountId(accountId)) {
			throw ProfileLinkServiceException.profileNotFound();
		}
		var profileLink = profileLinkRepository.findOwnedById(profileLinkId, accountId)
				.orElseThrow(ProfileLinkServiceException::linkNotFound);

		profileLink.update(
				request.platform(),
				request.label(),
				validateUrl(request.url()),
				profileLink.getDisplayOrder());
		profileLinkRepository.flush();
		return findResponse(profileLinkId, accountId);
	}

	@Transactional
	public void deleteLink(UUID accountId, UUID profileLinkId) {
		if (!profileRepository.existsByAccountId(accountId)) {
			throw ProfileLinkServiceException.profileNotFound();
		}
		var profileLink = profileLinkRepository.findOwnedById(profileLinkId, accountId)
				.orElseThrow(ProfileLinkServiceException::linkNotFound);
		var profileId = profileLink.getProfileEntity().getId();
		var removedDisplayOrder = profileLink.getDisplayOrder();

		profileLinkRepository.delete(profileLink);
		profileLinkRepository.shiftDisplayOrdersAfter(profileId, removedDisplayOrder);
	}

	private ProfileLinkResponse findResponse(UUID profileLinkId, UUID accountId) {
		return profileLinkRepository.findResponseById(profileLinkId, accountId)
				.orElseThrow(ProfileLinkServiceException::linkNotFound);
	}

	private static String validateUrl(String value) {
		if (value == null || value.isBlank()) {
			throw ProfileLinkServiceException.invalidUrl();
		}
		var normalized = value.trim();
		try {
			var uri = new URI(normalized);
			var scheme = uri.getScheme();
			if (scheme == null
					|| uri.getHost() == null
					|| !(scheme.toLowerCase(Locale.ROOT).equals("http")
					|| scheme.toLowerCase(Locale.ROOT).equals("https"))) {
				throw ProfileLinkServiceException.invalidUrl();
			}
			return normalized;
		} catch (URISyntaxException exception) {
			throw ProfileLinkServiceException.invalidUrl();
		}
	}
}
