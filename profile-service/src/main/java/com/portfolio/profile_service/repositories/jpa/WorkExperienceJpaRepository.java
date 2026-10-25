package com.portfolio.profile_service.repositories.jpa;

import com.portfolio.profile_service.dto.response.WorkExperienceResponse;
import com.portfolio.profile_service.entities.WorkExperienceEntity;
import com.portfolio.profile_service.repositories.WorkExperienceRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WorkExperienceJpaRepository implements WorkExperienceRepository {

	private static final String RESPONSE_PROJECTION = """
			select new com.portfolio.profile_service.dto.response.WorkExperienceResponse(
				workExperience.id,
				workExperience.companyName,
				workExperience.positionTitle,
				workExperience.employmentType,
				workExperience.location,
				workExperience.description,
				workExperience.startedOn,
				workExperience.endedOn,
				workExperience.currentPosition,
				workExperience.displayOrder,
				workExperience.createdAt,
				workExperience.updatedAt
			)
			from WorkExperienceEntity workExperience
			join workExperience.profileEntity profile
			""";

	private final EntityManager entityManager;

	@Override
	public int nextDisplayOrder(UUID profileId) {
		return entityManager.createQuery(
				"""
						select coalesce(max(workExperience.displayOrder), -1) + 1
						from WorkExperienceEntity workExperience
						where workExperience.profileEntity.id = :profileId
						""",
				Integer.class)
				.setParameter("profileId", profileId)
				.getSingleResult();
	}

	@Override
	public WorkExperienceEntity save(WorkExperienceEntity workExperienceEntity) {
		entityManager.persist(workExperienceEntity);
		entityManager.flush();
		return workExperienceEntity;
	}

	@Override
	public Optional<WorkExperienceEntity> findOwnedById(UUID workExperienceId, UUID accountId) {
		return entityManager.createQuery(
				"""
						select workExperience
						from WorkExperienceEntity workExperience
						join workExperience.profileEntity profile
						where workExperience.id = :workExperienceId
							and profile.accountId = :accountId
						""",
				WorkExperienceEntity.class)
				.setParameter("workExperienceId", workExperienceId)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<WorkExperienceResponse> findResponseById(UUID workExperienceId, UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where workExperience.id = :workExperienceId
							and profile.accountId = :accountId
						""",
				WorkExperienceResponse.class)
				.setParameter("workExperienceId", workExperienceId)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<WorkExperienceResponse> findAllResponsesByAccountId(UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profile.accountId = :accountId
						order by workExperience.displayOrder, workExperience.createdAt
						""",
				WorkExperienceResponse.class)
				.setParameter("accountId", accountId)
				.getResultList();
	}

	@Override
	public List<WorkExperienceResponse> findAllResponsesByProfileId(UUID profileId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profile.id = :profileId
						order by workExperience.displayOrder, workExperience.createdAt
						""",
				WorkExperienceResponse.class)
				.setParameter("profileId", profileId)
				.getResultList();
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void delete(WorkExperienceEntity workExperienceEntity) {
		entityManager.remove(workExperienceEntity);
		entityManager.flush();
	}

	@Override
	public void shiftDisplayOrdersAfter(UUID profileId, int removedDisplayOrder) {
		entityManager.createQuery(
				"""
						update WorkExperienceEntity workExperience
						set workExperience.displayOrder = workExperience.displayOrder - 1
						where workExperience.profileEntity.id = :profileId
							and workExperience.displayOrder > :removedDisplayOrder
						""")
				.setParameter("profileId", profileId)
				.setParameter("removedDisplayOrder", removedDisplayOrder)
				.executeUpdate();
	}
}
