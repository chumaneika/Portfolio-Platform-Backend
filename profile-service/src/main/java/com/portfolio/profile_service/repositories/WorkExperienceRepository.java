package com.portfolio.profile_service.repositories;

import com.portfolio.profile_service.dto.response.WorkExperienceResponse;
import com.portfolio.profile_service.entities.WorkExperienceEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkExperienceRepository {

	int nextDisplayOrder(UUID profileId);

	WorkExperienceEntity save(WorkExperienceEntity workExperienceEntity);

	Optional<WorkExperienceEntity> findOwnedById(UUID workExperienceId, UUID accountId);

	Optional<WorkExperienceResponse> findResponseById(UUID workExperienceId, UUID accountId);

	List<WorkExperienceResponse> findAllResponsesByAccountId(UUID accountId);

	List<WorkExperienceResponse> findAllResponsesByProfileId(UUID profileId);

	void flush();

	void delete(WorkExperienceEntity workExperienceEntity);

	void shiftDisplayOrdersAfter(UUID profileId, int removedDisplayOrder);
}
