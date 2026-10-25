package com.portfolio.profile_service.services;

import com.portfolio.profile_service.dto.request.WorkExperienceRequest;
import com.portfolio.profile_service.dto.response.WorkExperienceResponse;
import com.portfolio.profile_service.entities.WorkExperienceEntity;
import com.portfolio.profile_service.repositories.ProfileRepository;
import com.portfolio.profile_service.repositories.WorkExperienceRepository;
import com.portfolio.profile_service.services.exceptions.WorkExperienceServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkExperienceService {

	private final ProfileRepository profileRepository;
	private final WorkExperienceRepository workExperienceRepository;

	@Transactional
	public WorkExperienceResponse createWorkExperience(UUID accountId, WorkExperienceRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var profile = profileRepository.findByAccountId(accountId)
				.orElseThrow(WorkExperienceServiceException::profileNotFound);
		var workExperience = new WorkExperienceEntity(
				profile,
				request.companyName(),
				request.positionTitle(),
				request.employmentType(),
				request.startedOn(),
				workExperienceRepository.nextDisplayOrder(profile.getId()));

		applyRequest(workExperience, request);
		workExperienceRepository.save(workExperience);
		return findResponse(workExperience.getId(), accountId);
	}

	@Transactional(readOnly = true)
	public List<WorkExperienceResponse> getWorkExperiences(UUID accountId) {
		if (!profileRepository.existsByAccountId(accountId)) {
			throw WorkExperienceServiceException.profileNotFound();
		}
		return workExperienceRepository.findAllResponsesByAccountId(accountId);
	}

	@Transactional
	public WorkExperienceResponse updateWorkExperience(
			UUID accountId,
			UUID workExperienceId,
			WorkExperienceRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (!profileRepository.existsByAccountId(accountId)) {
			throw WorkExperienceServiceException.profileNotFound();
		}
		var workExperience = workExperienceRepository.findOwnedById(workExperienceId, accountId)
				.orElseThrow(WorkExperienceServiceException::workExperienceNotFound);

		applyRequest(workExperience, request);
		workExperienceRepository.flush();
		return findResponse(workExperienceId, accountId);
	}

	@Transactional
	public void deleteWorkExperience(UUID accountId, UUID workExperienceId) {
		if (!profileRepository.existsByAccountId(accountId)) {
			throw WorkExperienceServiceException.profileNotFound();
		}
		var workExperience = workExperienceRepository.findOwnedById(workExperienceId, accountId)
				.orElseThrow(WorkExperienceServiceException::workExperienceNotFound);
		var profileId = workExperience.getProfileEntity().getId();
		var removedDisplayOrder = workExperience.getDisplayOrder();

		workExperienceRepository.delete(workExperience);
		workExperienceRepository.shiftDisplayOrdersAfter(profileId, removedDisplayOrder);
	}

	private WorkExperienceResponse findResponse(UUID workExperienceId, UUID accountId) {
		return workExperienceRepository.findResponseById(workExperienceId, accountId)
				.orElseThrow(WorkExperienceServiceException::workExperienceNotFound);
	}

	private static void applyRequest(
			WorkExperienceEntity workExperience,
			WorkExperienceRequest request) {
		workExperience.updateDetails(
				request.companyName(),
				request.positionTitle(),
				request.employmentType(),
				request.location(),
				request.description());
		try {
			workExperience.setPeriod(
					request.startedOn(),
					request.endedOn(),
					Objects.requireNonNull(
							request.currentPosition(),
							"currentPosition must not be null"));
		} catch (IllegalArgumentException exception) {
			throw WorkExperienceServiceException.invalidPeriod(exception.getMessage());
		}
	}

}
