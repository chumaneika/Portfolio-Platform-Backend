package com.portfolio.profile_service.repositories;

import com.portfolio.profile_service.dto.response.ProfileResponse;
import com.portfolio.profile_service.entities.ProfileEntity;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {

	boolean existsByAccountId(UUID accountId);

	Optional<ProfileEntity> findByAccountId(UUID accountId);

	ProfileEntity save(ProfileEntity profileEntity);

	Optional<ProfileResponse> findProfileResponseByAccountId(UUID accountId);

	Optional<ProfileResponse> findProfileResponseById(UUID profileId);

	void flush();

	void delete(ProfileEntity profileEntity);
}
