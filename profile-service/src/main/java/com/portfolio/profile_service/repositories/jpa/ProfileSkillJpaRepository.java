package com.portfolio.profile_service.repositories.jpa;

import com.portfolio.profile_service.dto.response.ProfileSkillResponse;
import com.portfolio.profile_service.entities.ProfileSkillEntity;
import com.portfolio.profile_service.repositories.ProfileSkillRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProfileSkillJpaRepository implements ProfileSkillRepository {

	private static final String RESPONSE_PROJECTION = """
			select new com.portfolio.profile_service.dto.response.ProfileSkillResponse(
				profileSkill.id.skillCode,
				profileSkill.displayName,
				profileSkill.category,
				profileSkill.displayOrder
			)
			from ProfileSkillEntity profileSkill
			join profileSkill.profileEntity profile
			""";

	private final EntityManager entityManager;

	@Override
	public int nextDisplayOrder(UUID profileId) {
		return entityManager.createQuery(
				"""
						select coalesce(max(profileSkill.displayOrder), -1) + 1
						from ProfileSkillEntity profileSkill
						where profileSkill.profileEntity.id = :profileId
						""",
				Integer.class)
				.setParameter("profileId", profileId)
				.getSingleResult();
	}

	@Override
	public boolean existsBySkillCode(String skillCode, UUID accountId) {
		return entityManager.createQuery(
				"""
						select count(profileSkill)
						from ProfileSkillEntity profileSkill
						join profileSkill.profileEntity profile
						where profileSkill.id.skillCode = :skillCode
							and profile.accountId = :accountId
						""",
				Long.class)
				.setParameter("skillCode", skillCode)
				.setParameter("accountId", accountId)
				.getSingleResult() > 0;
	}

	@Override
	public ProfileSkillEntity save(ProfileSkillEntity profileSkillEntity) {
		entityManager.persist(profileSkillEntity);
		entityManager.flush();
		return profileSkillEntity;
	}

	@Override
	public Optional<ProfileSkillEntity> findOwnedBySkillCode(String skillCode, UUID accountId) {
		return entityManager.createQuery(
				"""
						select profileSkill
						from ProfileSkillEntity profileSkill
						join profileSkill.profileEntity profile
						where profileSkill.id.skillCode = :skillCode
							and profile.accountId = :accountId
						""",
				ProfileSkillEntity.class)
				.setParameter("skillCode", skillCode)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<ProfileSkillResponse> findResponseBySkillCode(String skillCode, UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profileSkill.id.skillCode = :skillCode
							and profile.accountId = :accountId
						""",
				ProfileSkillResponse.class)
				.setParameter("skillCode", skillCode)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<ProfileSkillResponse> findAllResponsesByAccountId(UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profile.accountId = :accountId
						order by profileSkill.displayOrder, profileSkill.id.skillCode
						""",
				ProfileSkillResponse.class)
				.setParameter("accountId", accountId)
				.getResultList();
	}

	@Override
	public List<ProfileSkillResponse> findAllResponsesByProfileId(UUID profileId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profile.id = :profileId
						order by profileSkill.displayOrder, profileSkill.id.skillCode
						""",
				ProfileSkillResponse.class)
				.setParameter("profileId", profileId)
				.getResultList();
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void delete(ProfileSkillEntity profileSkillEntity) {
		entityManager.remove(profileSkillEntity);
		entityManager.flush();
	}

	@Override
	public void shiftDisplayOrdersAfter(UUID profileId, int removedDisplayOrder) {
		entityManager.createQuery(
				"""
						update ProfileSkillEntity profileSkill
						set profileSkill.displayOrder = profileSkill.displayOrder - 1
						where profileSkill.profileEntity.id = :profileId
							and profileSkill.displayOrder > :removedDisplayOrder
						""")
				.setParameter("profileId", profileId)
				.setParameter("removedDisplayOrder", removedDisplayOrder)
				.executeUpdate();
	}
}
