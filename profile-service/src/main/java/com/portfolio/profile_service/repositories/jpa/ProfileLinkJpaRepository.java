package com.portfolio.profile_service.repositories.jpa;

import com.portfolio.profile_service.dto.response.ProfileLinkResponse;
import com.portfolio.profile_service.entities.ProfileLinkEntity;
import com.portfolio.profile_service.repositories.ProfileLinkRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProfileLinkJpaRepository implements ProfileLinkRepository {

	private static final String RESPONSE_PROJECTION = """
			select new com.portfolio.profile_service.dto.response.ProfileLinkResponse(
				profileLink.id,
				profileLink.platform,
				profileLink.label,
				profileLink.url,
				profileLink.displayOrder
			)
			from ProfileLinkEntity profileLink
			join profileLink.profileEntity profile
			""";

	private final EntityManager entityManager;

	@Override
	public int nextDisplayOrder(UUID profileId) {
		return entityManager.createQuery(
				"""
						select coalesce(max(profileLink.displayOrder), -1) + 1
						from ProfileLinkEntity profileLink
						where profileLink.profileEntity.id = :profileId
						""",
				Integer.class)
				.setParameter("profileId", profileId)
				.getSingleResult();
	}

	@Override
	public ProfileLinkEntity save(ProfileLinkEntity profileLinkEntity) {
		entityManager.persist(profileLinkEntity);
		entityManager.flush();
		return profileLinkEntity;
	}

	@Override
	public Optional<ProfileLinkEntity> findOwnedById(UUID profileLinkId, UUID accountId) {
		return entityManager.createQuery(
				"""
						select profileLink
						from ProfileLinkEntity profileLink
						join profileLink.profileEntity profile
						where profileLink.id = :profileLinkId
							and profile.accountId = :accountId
						""",
				ProfileLinkEntity.class)
				.setParameter("profileLinkId", profileLinkId)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<ProfileLinkResponse> findResponseById(UUID profileLinkId, UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profileLink.id = :profileLinkId
							and profile.accountId = :accountId
						""",
				ProfileLinkResponse.class)
				.setParameter("profileLinkId", profileLinkId)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<ProfileLinkResponse> findAllResponsesByAccountId(UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profile.accountId = :accountId
						order by profileLink.displayOrder, profileLink.id
						""",
				ProfileLinkResponse.class)
				.setParameter("accountId", accountId)
				.getResultList();
	}

	@Override
	public List<ProfileLinkResponse> findAllResponsesByProfileId(UUID profileId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profile.id = :profileId
						order by profileLink.displayOrder, profileLink.id
						""",
				ProfileLinkResponse.class)
				.setParameter("profileId", profileId)
				.getResultList();
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void delete(ProfileLinkEntity profileLinkEntity) {
		entityManager.remove(profileLinkEntity);
		entityManager.flush();
	}

	@Override
	public void shiftDisplayOrdersAfter(UUID profileId, int removedDisplayOrder) {
		entityManager.createQuery(
				"""
						update ProfileLinkEntity profileLink
						set profileLink.displayOrder = profileLink.displayOrder - 1
						where profileLink.profileEntity.id = :profileId
							and profileLink.displayOrder > :removedDisplayOrder
						""")
				.setParameter("profileId", profileId)
				.setParameter("removedDisplayOrder", removedDisplayOrder)
				.executeUpdate();
	}
}
