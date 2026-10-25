package com.portfolio.profile_service.repositories.jpa;

import com.portfolio.profile_service.dto.response.EducationResponse;
import com.portfolio.profile_service.entities.EducationEntity;
import com.portfolio.profile_service.repositories.EducationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EducationJpaRepository implements EducationRepository {

	private static final String RESPONSE_PROJECTION = """
			select new com.portfolio.profile_service.dto.response.EducationResponse(
				education.id,
				education.institutionName,
				education.degree,
				education.fieldOfStudy,
				education.description,
				education.startedOn,
				education.endedOn,
				education.currentEducation,
				education.displayOrder,
				education.createdAt,
				education.updatedAt
			)
			from EducationEntity education
			join education.profileEntity profile
			""";

	private final EntityManager entityManager;

	@Override
	public int nextDisplayOrder(UUID profileId) {
		return entityManager.createQuery(
				"""
						select coalesce(max(education.displayOrder), -1) + 1
						from EducationEntity education
						where education.profileEntity.id = :profileId
						""",
				Integer.class)
				.setParameter("profileId", profileId)
				.getSingleResult();
	}

	@Override
	public EducationEntity save(EducationEntity educationEntity) {
		entityManager.persist(educationEntity);
		entityManager.flush();
		return educationEntity;
	}

	@Override
	public Optional<EducationEntity> findOwnedById(UUID educationId, UUID accountId) {
		return entityManager.createQuery(
				"""
						select education
						from EducationEntity education
						join education.profileEntity profile
						where education.id = :educationId
							and profile.accountId = :accountId
						""",
				EducationEntity.class)
				.setParameter("educationId", educationId)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public Optional<EducationResponse> findResponseById(UUID educationId, UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where education.id = :educationId
							and profile.accountId = :accountId
						""",
				EducationResponse.class)
				.setParameter("educationId", educationId)
				.setParameter("accountId", accountId)
				.getResultStream()
				.findFirst();
	}

	@Override
	public List<EducationResponse> findAllResponsesByAccountId(UUID accountId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profile.accountId = :accountId
						order by education.displayOrder, education.createdAt
						""",
				EducationResponse.class)
				.setParameter("accountId", accountId)
				.getResultList();
	}

	@Override
	public List<EducationResponse> findAllResponsesByProfileId(UUID profileId) {
		return entityManager.createQuery(
				RESPONSE_PROJECTION + """
						where profile.id = :profileId
						order by education.displayOrder, education.createdAt
						""",
				EducationResponse.class)
				.setParameter("profileId", profileId)
				.getResultList();
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void delete(EducationEntity educationEntity) {
		entityManager.remove(educationEntity);
		entityManager.flush();
	}

	@Override
	public void shiftDisplayOrdersAfter(UUID profileId, int removedDisplayOrder) {
		entityManager.createQuery(
				"""
						update EducationEntity education
						set education.displayOrder = education.displayOrder - 1
						where education.profileEntity.id = :profileId
							and education.displayOrder > :removedDisplayOrder
						""")
				.setParameter("profileId", profileId)
				.setParameter("removedDisplayOrder", removedDisplayOrder)
				.executeUpdate();
	}
}
