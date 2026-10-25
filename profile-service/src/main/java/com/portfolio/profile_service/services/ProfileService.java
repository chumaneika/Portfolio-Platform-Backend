package com.portfolio.profile_service.services;

import com.portfolio.profile_service.dto.request.ProfileRequest;
import com.portfolio.profile_service.dto.response.ProfileResponse;
import com.portfolio.profile_service.entities.ProfileEntity;
import com.portfolio.profile_service.repositories.ProfileRepository;
import com.portfolio.profile_service.services.exceptions.ProfileServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private final ProfileRepository profileRepository;

	@Transactional
	public ProfileResponse updateProfile(UUID accountId, ProfileRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var profile = profileRepository.findByAccountId(accountId)
				.orElseThrow(ProfileServiceException::profileNotFound);

		profile.updateName(request.firstName(), request.lastName());
		profile.updatePresentation(request.headline(), request.bio(), request.location());
		profile.updateMedia(request.avatarUrl(), request.resumeUrl());
		profile.setAvailableForWork(Objects.requireNonNull(
				request.availableForWork(),
				"availableForWork must not be null"));

		profileRepository.flush();
		return profileRepository.findProfileResponseByAccountId(accountId)
				.orElseThrow(ProfileServiceException::profileNotFound);
	}

	@Transactional
	public UUID createProfile(UUID accountId, String firstName, String lastName) {
		if (profileRepository.existsByAccountId(accountId)) {
			throw ProfileServiceException.profileAlreadyExists();
		}

		try {
			var profile = profileRepository.save(new ProfileEntity(accountId, firstName, lastName));
			return profile.getId();
		} catch (DataIntegrityViolationException exception) {
			throw ProfileServiceException.profileAlreadyExists();
		}
	}

	@Transactional(readOnly = true)
	public ProfileResponse getProfile(UUID accountId) {
		return profileRepository.findProfileResponseByAccountId(accountId)
				.orElseThrow(ProfileServiceException::profileNotFound);
	}

	@Transactional
	public void deleteProfile(UUID accountId) {
		var profile = profileRepository.findByAccountId(accountId)
				.orElseThrow(ProfileServiceException::profileNotFound);
		profileRepository.delete(profile);
	}

}
