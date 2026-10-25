package com.portfolio.profile_service.repositories;

import com.portfolio.profile_service.dto.response.ProfileLinkResponse;
import com.portfolio.profile_service.entities.ProfileLinkEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileLinkRepository {

	int nextDisplayOrder(UUID profileId);

	ProfileLinkEntity save(ProfileLinkEntity profileLinkEntity);

	Optional<ProfileLinkEntity> findOwnedById(UUID profileLinkId, UUID accountId);

	Optional<ProfileLinkResponse> findResponseById(UUID profileLinkId, UUID accountId);

	List<ProfileLinkResponse> findAllResponsesByAccountId(UUID accountId);

	List<ProfileLinkResponse> findAllResponsesByProfileId(UUID profileId);

	void flush();

	void delete(ProfileLinkEntity profileLinkEntity);

	void shiftDisplayOrdersAfter(UUID profileId, int removedDisplayOrder);
}
