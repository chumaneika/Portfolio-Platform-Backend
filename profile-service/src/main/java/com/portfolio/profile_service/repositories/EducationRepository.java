package com.portfolio.profile_service.repositories;

import com.portfolio.profile_service.dto.response.EducationResponse;
import com.portfolio.profile_service.entities.EducationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EducationRepository {

	int nextDisplayOrder(UUID profileId);

	EducationEntity save(EducationEntity educationEntity);

	Optional<EducationEntity> findOwnedById(UUID educationId, UUID accountId);

	Optional<EducationResponse> findResponseById(UUID educationId, UUID accountId);

	List<EducationResponse> findAllResponsesByAccountId(UUID accountId);

	List<EducationResponse> findAllResponsesByProfileId(UUID profileId);

	void flush();

	void delete(EducationEntity educationEntity);

	void shiftDisplayOrdersAfter(UUID profileId, int removedDisplayOrder);
}
