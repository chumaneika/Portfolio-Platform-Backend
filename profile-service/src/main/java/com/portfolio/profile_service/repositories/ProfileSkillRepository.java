package com.portfolio.profile_service.repositories;

import com.portfolio.profile_service.dto.response.ProfileSkillResponse;
import com.portfolio.profile_service.entities.ProfileSkillEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileSkillRepository {

	int nextDisplayOrder(UUID profileId);

	boolean existsBySkillCode(String skillCode, UUID accountId);

	ProfileSkillEntity save(ProfileSkillEntity profileSkillEntity);

	Optional<ProfileSkillEntity> findOwnedBySkillCode(String skillCode, UUID accountId);

	Optional<ProfileSkillResponse> findResponseBySkillCode(String skillCode, UUID accountId);

	List<ProfileSkillResponse> findAllResponsesByAccountId(UUID accountId);

	List<ProfileSkillResponse> findAllResponsesByProfileId(UUID profileId);

	void flush();

	void delete(ProfileSkillEntity profileSkillEntity);

	void shiftDisplayOrdersAfter(UUID profileId, int removedDisplayOrder);
}
