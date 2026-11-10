package ca.bc.gov.educ.challengeReports.api.service.v1;

import ca.bc.gov.educ.challengeReports.api.repository.v1.ChallengeReportsPostedStudentRepository;
import ca.bc.gov.educ.challengeReports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challengeReports.api.rest.RestUtils;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.IndependentSchoolFundingGroupSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FundingGroupForCourseTest {

  private final ChallengeReportsService challengeReportsService = new ChallengeReportsService(mock(ChallengeReportsSessionRepository.class), mock(ChallengeReportsPostedStudentRepository.class), mock(RestUtils.class));

  @Test
  void whenCourseGradeMatchesSchoolGrade_returnsThatGradesGroup() {
    var groups = List.of(group("GRADE11", "GROUP2"), group("GRADE12", "GROUP1"));

    assertThat(challengeReportsService.getFundingGroupForCourse(groups, "MATH 12")).isEqualTo("GROUP1");
  }

  @Test
  void whenCourseGradeHasNoMatchingSchoolGrade_returnsNull() {
    var groups = List.of(group("GRADE11", "GROUP2"));

    assertThat(challengeReportsService.getFundingGroupForCourse(groups, "MATH 12")).isNull();
  }

  @Test
  void whenCourseHasNoGrade_prefersGroup1FromSeniorGrades() {
    var groups = List.of(group("GRADE10", "GROUP2"), group("GRADE12", "GROUP1"), group("GRADE07", "GROUP1"));

    assertThat(challengeReportsService.getFundingGroupForCourse(groups, "LTE")).isEqualTo("GROUP1");
  }

  @Test
  void whenCourseHasNoGrade_fallsBackToGroup2FromSeniorGrades() {
    var groups = List.of(group("GRADE11", "GROUP2"), group("GRADE07", "GROUP1"));

    assertThat(challengeReportsService.getFundingGroupForCourse(groups, null)).isEqualTo("GROUP2");
  }

  @Test
  void whenCourseHasNoGradeAndNoSeniorGroups_returnsEmpty() {
    var groups = List.of(group("GRADE07", "GROUP1"));

    assertThat(challengeReportsService.getFundingGroupForCourse(groups, "")).isEmpty();
  }

  private static IndependentSchoolFundingGroupSnapshot group(String schoolGradeCode, String fundingGroupCode) {
    var group = new IndependentSchoolFundingGroupSnapshot();
    group.setSchoolGradeCode(schoolGradeCode);
    group.setSchoolFundingGroupCode(fundingGroupCode);
    return group;
  }
}
