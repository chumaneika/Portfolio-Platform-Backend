package com.portfolio.profile_service.repositories.jpa;

import com.portfolio.profile_service.dto.response.ProfileResponse;
import com.portfolio.profile_service.entities.ProfileEntity;
import com.portfolio.profile_service.repositories.ProfileRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProfileJpaRepository implements ProfileRepository {

	private static final String RESPONSE_PROJECTION = """
			select new com.portfolio.profile_service.dto.response.ProfileResponse(
				profileEntity.id,
				profileEntity.firstName,
				profileEntity.lastName,
				profileEntity.headline,
				profileEntity.bio,
				profileEntity.location,
				profileEntity.avatarUrl,
				profileEntity.resumeUrl,
				profileEntity.availableForWork,
				profileEntity.createdAt,
				profileEntity.updatedAt
			)
			from ProfileEntity profileEntity
			""";

	private final EntityManager entityManager;

	@Override
	public Optional<ProfileResponse> findProfileResponseByAccountId(UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profileEntity.accountId = :accountId
						""",
				ProfileResponse.class)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<ProfileResponse> findProfileResponseById(UUID profileId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profileEntity.id = :profileId
						""",
				ProfileResponse.class)
				.setParameter("profileId", profileId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public boolean existsByAccountId(UUID accountId) {
		return entityManager.createQuery(
				"""
						select count(profileEntity)
						from ProfileEntity profileEntity
						where profileEntity.accountId = :accountId
						""",
				Long.class)
				.setParameter("accountId", accountId)
				.getSingleResult() > 0;
	}

	@Override
	public Optional<ProfileEntity> findByAccountId(UUID accountId) {
		return entityManager.createQuery(
				"""
						select profileEntity
						from ProfileEntity profileEntity
						where profileEntity.accountId = :accountId
						""",
				ProfileEntity.class)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public ProfileEntity save(ProfileEntity profileEntity) {
		entityManager.persist(profileEntity);
		entityManager.flush();
		return profileEntity;
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void delete(ProfileEntity profileEntity) {
		entityManager.remove(profileEntity);
		entityManager.flush();
	}
}
