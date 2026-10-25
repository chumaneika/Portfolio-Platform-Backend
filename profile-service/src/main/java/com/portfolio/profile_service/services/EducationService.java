package com.portfolio.profile_service.services;

import com.portfolio.profile_service.dto.request.EducationRequest;
import com.portfolio.profile_service.dto.response.EducationResponse;
import com.portfolio.profile_service.entities.EducationEntity;
import com.portfolio.profile_service.repositories.EducationRepository;
import com.portfolio.profile_service.repositories.ProfileRepository;
import com.portfolio.profile_service.services.exceptions.EducationServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EducationService {

	private final ProfileRepository profileRepository;
	private final EducationRepository educationRepository;

	@Transactional
	public EducationResponse createEducation(UUID accountId, EducationRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var profile = profileRepository.findByAccountId(accountId)
				.orElseThrow(EducationServiceException::profileNotFound);
		var education = new EducationEntity(
				profile,
				request.institutionName(),
				educationRepository.nextDisplayOrder(profile.getId()));

		applyRequest(education, request);
		educationRepository.save(education);
		return findResponse(education.getId(), accountId);
	}

	@Transactional(readOnly = true)
	public List<EducationResponse> getEducations(UUID accountId) {
		if (!profileRepository.existsByAccountId(accountId)) {
			throw EducationServiceException.profileNotFound();
		}
		return educationRepository.findAllResponsesByAccountId(accountId);
	}

	@Transactional
	public EducationResponse updateEducation(
			UUID accountId,
			UUID educationId,
			EducationRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (!profileRepository.existsByAccountId(accountId)) {
			throw EducationServiceException.profileNotFound();
		}
		var education = educationRepository.findOwnedById(educationId, accountId)
				.orElseThrow(EducationServiceException::educationNotFound);

		applyRequest(education, request);
		educationRepository.flush();
		return findResponse(educationId, accountId);
	}

	@Transactional
	public void deleteEducation(UUID accountId, UUID educationId) {
		if (!profileRepository.existsByAccountId(accountId)) {
			throw EducationServiceException.profileNotFound();
		}
		var education = educationRepository.findOwnedById(educationId, accountId)
				.orElseThrow(EducationServiceException::educationNotFound);
		var profileId = education.getProfileEntity().getId();
		var removedDisplayOrder = education.getDisplayOrder();

		educationRepository.delete(education);
		educationRepository.shiftDisplayOrdersAfter(profileId, removedDisplayOrder);
	}

	private EducationResponse findResponse(UUID educationId, UUID accountId) {
		return educationRepository.findResponseById(educationId, accountId)
				.orElseThrow(EducationServiceException::educationNotFound);
	}

	private static void applyRequest(EducationEntity education, EducationRequest request) {
		education.updateDetails(
				request.institutionName(),
				request.degree(),
				request.fieldOfStudy(),
				request.description());
		try {
			education.setPeriod(
					request.startedOn(),
					request.endedOn(),
					Objects.requireNonNull(
							request.currentEducation(),
							"currentEducation must not be null"));
		} catch (IllegalArgumentException exception) {
			throw EducationServiceException.invalidPeriod(exception.getMessage());
		}
	}
}
