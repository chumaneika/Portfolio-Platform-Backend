package com.portfolio.profile_service.services;

import com.portfolio.profile_service.dto.request.CreateProfileSkillRequest;
import com.portfolio.profile_service.dto.request.ProfileSkillRequest;
import com.portfolio.profile_service.dto.response.ProfileSkillResponse;
import com.portfolio.profile_service.entities.ProfileSkillEntity;
import com.portfolio.profile_service.repositories.ProfileRepository;
import com.portfolio.profile_service.repositories.ProfileSkillRepository;
import com.portfolio.profile_service.services.exceptions.ProfileSkillServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileSkillService {

	private final ProfileRepository profileRepository;
	private final ProfileSkillRepository profileSkillRepository;

	@Transactional
	public ProfileSkillResponse createSkill(UUID accountId, CreateProfileSkillRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var profile = profileRepository.findByAccountId(accountId)
				.orElseThrow(ProfileSkillServiceException::profileNotFound);
		var skillCode = normalizeSkillCode(request.skillCode());
		if (profileSkillRepository.existsBySkillCode(skillCode, accountId)) {
			throw ProfileSkillServiceException.skillAlreadyExists();
		}
		var profileSkill = new ProfileSkillEntity(
				profile,
				skillCode,
				request.displayName(),
				request.category(),
				profileSkillRepository.nextDisplayOrder(profile.getId()));

		try {
			profileSkillRepository.save(profileSkill);
		} catch (DataIntegrityViolationException exception) {
			throw ProfileSkillServiceException.skillAlreadyExists();
		}
		return findResponse(skillCode, accountId);
	}

	@Transactional(readOnly = true)
	public List<ProfileSkillResponse> getSkills(UUID accountId) {
		if (!profileRepository.existsByAccountId(accountId)) {
			throw ProfileSkillServiceException.profileNotFound();
		}
		return profileSkillRepository.findAllResponsesByAccountId(accountId);
	}

	@Transactional
	public ProfileSkillResponse updateSkill(
			UUID accountId,
			String skillCode,
			ProfileSkillRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (!profileRepository.existsByAccountId(accountId)) {
			throw ProfileSkillServiceException.profileNotFound();
		}
		var normalizedCode = normalizeSkillCode(skillCode);
		var profileSkill = profileSkillRepository.findOwnedBySkillCode(normalizedCode, accountId)
				.orElseThrow(ProfileSkillServiceException::skillNotFound);

		profileSkill.updatePresentation(
				request.displayName(),
				request.category(),
				profileSkill.getDisplayOrder());
		profileSkillRepository.flush();
		return findResponse(normalizedCode, accountId);
	}

	@Transactional
	public void deleteSkill(UUID accountId, String skillCode) {
		if (!profileRepository.existsByAccountId(accountId)) {
			throw ProfileSkillServiceException.profileNotFound();
		}
		var normalizedCode = normalizeSkillCode(skillCode);
		var profileSkill = profileSkillRepository.findOwnedBySkillCode(normalizedCode, accountId)
				.orElseThrow(ProfileSkillServiceException::skillNotFound);
		var profileId = profileSkill.getProfileEntity().getId();
		var removedDisplayOrder = profileSkill.getDisplayOrder();

		profileSkillRepository.delete(profileSkill);
		profileSkillRepository.shiftDisplayOrdersAfter(profileId, removedDisplayOrder);
	}

	private ProfileSkillResponse findResponse(String skillCode, UUID accountId) {
		return profileSkillRepository.findResponseBySkillCode(skillCode, accountId)
				.orElseThrow(ProfileSkillServiceException::skillNotFound);
	}

	private static String normalizeSkillCode(String skillCode) {
		if (skillCode == null || skillCode.isBlank()) {
			throw ProfileSkillServiceException.skillNotFound();
		}
		return skillCode.trim().toLowerCase(Locale.ROOT);
	}
}
