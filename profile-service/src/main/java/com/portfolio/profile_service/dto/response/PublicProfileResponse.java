package com.portfolio.profile_service.dto.response;

import java.util.List;

public record PublicProfileResponse(
		ProfileResponse profile,
		List<WorkExperienceResponse> workExperiences,
		List<EducationResponse> educations,
		List<ProfileSkillResponse> skills,
		List<ProfileLinkResponse> links) {
}
