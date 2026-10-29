package com.portfolio.profile_service.services;

import com.portfolio.profile_service.dto.response.PublicProfileResponse;
import com.portfolio.profile_service.dto.response.ProfileResponse;
import com.portfolio.profile_service.repositories.EducationRepository;
import com.portfolio.profile_service.repositories.ProfileLinkRepository;
import com.portfolio.profile_service.repositories.ProfileRepository;
import com.portfolio.profile_service.repositories.ProfileSkillRepository;
import com.portfolio.profile_service.repositories.WorkExperienceRepository;
import com.portfolio.profile_service.services.exceptions.ProfileServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

	private final ProfileRepository profileRepository;
	private final WorkExperienceRepository workExperienceRepository;
	private final EducationRepository educationRepository;
	private final ProfileSkillRepository profileSkillRepository;
	private final ProfileLinkRepository profileLinkRepository;

	@Transactional(readOnly = true)
	public PublicProfileResponse getPublicProfile(UUID profileId) {
		var profile = profileRepository.findProfileResponseById(profileId)
				.orElseThrow(ProfileServiceException::profileNotFound);
		return buildPublicProfile(profile);
	}

	@Transactional(readOnly = true)
	public PublicProfileResponse getPublicProfileByAccountId(UUID accountId) {
		var profile = profileRepository.findProfileResponseByAccountId(accountId)
				.orElseThrow(ProfileServiceException::profileNotFound);
		return buildPublicProfile(profile);
	}

	private PublicProfileResponse buildPublicProfile(ProfileResponse profile) {
		var profileId = profile.id();
		return new PublicProfileResponse(
				profile,
				workExperienceRepository.findAllResponsesByProfileId(profileId),
				educationRepository.findAllResponsesByProfileId(profileId),
				profileSkillRepository.findAllResponsesByProfileId(profileId),
				profileLinkRepository.findAllResponsesByProfileId(profileId));
	}
}
