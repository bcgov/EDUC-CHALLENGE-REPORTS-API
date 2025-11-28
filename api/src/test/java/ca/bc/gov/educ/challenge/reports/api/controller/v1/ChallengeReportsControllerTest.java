package ca.bc.gov.educ.challenge.reports.api.controller.v1;

import ca.bc.gov.educ.challenge.reports.api.BaseChallengeReportsAPITest;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.URL;
import ca.bc.gov.educ.challenge.reports.api.mappers.v1.ChallengeReportSessionMapper;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsReportingPeriodEntity;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsReportingPeriodRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSagaEventRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSagaRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challenge.reports.api.rest.RestUtils;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsSession;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DistrictChallengeReportsCounts;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.PaginatedResponse;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.coreg.v1.CourseCode;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.gradstudent.v1.GraduationStudentPaginationRecord;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.gradstudent.v1.StudentCoursePagination;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.sdc.v1.Collection;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.studentapi.v1.Student;
import ca.bc.gov.educ.challenge.reports.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class ChallengeReportsControllerTest extends BaseChallengeReportsAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  ChallengeReportsReportingPeriodRepository challengeReportsReportingPeriodRepository;
  @Autowired
  ChallengeReportsSessionRepository challengeReportsSessionRepository;
  @Autowired
  ChallengeReportsSagaRepository challengeReportsSagaRepository;
  @Autowired
  ChallengeReportsSagaEventRepository challengeReportsSagaEventRepository;
  @Autowired
  RestUtils restUtils;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    challengeReportsSessionRepository.deleteAll();
    challengeReportsReportingPeriodRepository.deleteAll();
    challengeReportsSagaEventRepository.deleteAll();
    challengeReportsSagaRepository.deleteAll();
  }

  protected static final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Test
  void testUpdateSession_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_CHALLENGE_REPORTS";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var reportingPeriod = challengeReportsReportingPeriodRepository.save(createMockReportingPeriod());

    var session = challengeReportsSessionRepository.save(createMockSession(reportingPeriod));

    var structSession = ChallengeReportSessionMapper.mapper.toStructure(session);
    structSession.setCreateDate(null);
    structSession.setUpdateDate(null);
    var resultActions1 = this.mockMvc.perform(put(URL.BASE_URL + "/" + session.getChallengeReportsSessionID()).with(mockAuthority).content(JsonUtil.getJsonStringFromObject(structSession)).contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<ChallengeReportsSession>() {
    });

    assertThat(summary1).isNotNull();
  }

  @Test
  void testGetActiveSession_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_CHALLENGE_REPORTS";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var reportingPeriod = challengeReportsReportingPeriodRepository.save(createMockReportingPeriod());

    var session = challengeReportsSessionRepository.save(createMockSession(reportingPeriod));

    var structSession = ChallengeReportSessionMapper.mapper.toStructure(session);
    structSession.setCreateDate(null);
    structSession.setUpdateDate(null);
    var resultActions1 = this.mockMvc.perform(get(URL.BASE_URL + "/activeSession").with(mockAuthority).contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<ChallengeReportsSession>() {
    });

    assertThat(summary1).isNotNull();
  }

  @Test
  void testStartPrelimStageSession_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_CHALLENGE_REPORTS";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var reportingPeriod = challengeReportsReportingPeriodRepository.save(createMockReportingPeriod());

    var session = challengeReportsSessionRepository.save(createMockSession(reportingPeriod));

    var structSession = ChallengeReportSessionMapper.mapper.toStructure(session);
    structSession.setCreateDate(null);
    structSession.setUpdateDate(null);
    this.mockMvc.perform(post(URL.BASE_URL + "/preliminaryStage/ABC").with(mockAuthority).contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isAccepted());

    var sagas = challengeReportsSagaRepository.findAll();

    assertThat(sagas).isNotNull();
    assertThat(sagas.size()).isEqualTo(1);
  }

  @Test
  void testStartFinalStageSession_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_CHALLENGE_REPORTS";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var reportingPeriod = challengeReportsReportingPeriodRepository.save(createMockReportingPeriod());

    var session = challengeReportsSessionRepository.save(createMockSession(reportingPeriod));

    var structSession = ChallengeReportSessionMapper.mapper.toStructure(session);
    structSession.setCreateDate(null);
    structSession.setUpdateDate(null);
    this.mockMvc.perform(post(URL.BASE_URL + "/finalStage/ABC").with(mockAuthority).contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isAccepted());

    var sagas = challengeReportsSagaRepository.findAll();

    assertThat(sagas).isNotNull();
    assertThat(sagas.size()).isEqualTo(1);
  }

  @Test
  void testGetDistrictChallengeReportCounts_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_CHALLENGE_REPORTS";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    ChallengeReportsReportingPeriodEntity reportingPeriod = new ChallengeReportsReportingPeriodEntity();
    reportingPeriod.setSchoolYear("2024");
    reportingPeriod.setActiveFromDate(LocalDateTime.now().minusDays(1));
    reportingPeriod.setActiveToDate(LocalDateTime.now().plusDays(1));
    reportingPeriod.setCreateUser("ABC");
    reportingPeriod.setCreateDate(LocalDateTime.now());
    reportingPeriod.setUpdateUser("ABC");
    reportingPeriod.setUpdateDate(LocalDateTime.now());

    reportingPeriod = challengeReportsReportingPeriodRepository.save(reportingPeriod);

    ChallengeReportsSessionEntity session = new ChallengeReportsSessionEntity();
    session.setChallengeReportsPeriod(reportingPeriod);
    session.setChallengeReportsStatusCode("PRELIM");
    session.setFundingRate("234");
    session.setFinalDateForChanges(LocalDateTime.now());
    session.setCreateUser("ABC");
    session.setCreateDate(LocalDateTime.now());
    session.setUpdateUser("ABC");
    session.setUpdateDate(LocalDateTime.now());

    session = challengeReportsSessionRepository.save(session);

    var schoolID = UUID.randomUUID();
    var studentID = UUID.randomUUID();
    GraduationStudentPaginationRecord gradStudent = new GraduationStudentPaginationRecord();
    gradStudent.setPen("123456789");
    gradStudent.setSchoolOfRecordId(schoolID);
    gradStudent.setStudentGrade("12");
    gradStudent.setStudentStatus("CUR");
    gradStudent.setStudentID(studentID);
    gradStudent.setCreateUser("ABC");
    gradStudent.setUpdateUser("ABC");
    gradStudent.setCreateDate(LocalDateTime.now().toString());
    gradStudent.setUpdateDate(LocalDateTime.now().toString());

    var studentCoursePagination = new StudentCoursePagination();
    studentCoursePagination.setCourseID("MATH12");
    studentCoursePagination.setEquivOrChallenge("C");
    studentCoursePagination.setGradStudent(gradStudent);
    studentCoursePagination.setCredits(4);
    studentCoursePagination.setStudentExamId(null);
    studentCoursePagination.setFinalPercent(66);
    studentCoursePagination.setStudentCourseID(UUID.randomUUID());
    studentCoursePagination.setCourseSession("202507");

    when(this.restUtils.getChallengeReportGradStudentCoursesForYear(anyList(), anyList())).thenReturn(List.of(studentCoursePagination));

    Collection coll = new Collection();
    coll.setCollectionID(UUID.randomUUID().toString());
    when(this.restUtils.getLastSeptemberCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(coll)));
    when(this.restUtils.getLastFebruaryCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(coll)));

    var student = new Student();
    student.setStudentID(studentID.toString());
    student.setPen("123456789");
    student.setLegalFirstName("JOE");
    student.setLegalMiddleNames("JACK");
    student.setLegalLastName("SMITH");

    when(this.restUtils.getStudents(any(), anySet())).thenReturn(List.of(student));

    SchoolTombstone schoolTombstone = this.createMockSchool();
    schoolTombstone.setSchoolId(schoolID.toString());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

    var course = new CourseCode();
    course.setCourseID("MATH12");
    course.setExternalCode("MATH12");
    course.setOriginatingSystem("39");

    when(this.restUtils.getCoregCourseByID(anyString())).thenReturn(Optional.of(course));

    var districtID = schoolTombstone.getDistrictId();

    var structSession = ChallengeReportSessionMapper.mapper.toStructure(session);
    structSession.setCreateDate(null);
    structSession.setUpdateDate(null);
    var resultActions1 = this.mockMvc.perform(get(URL.BASE_URL + "/district/" + districtID).with(mockAuthority).contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DistrictChallengeReportsCounts>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getSchoolsWithCounts().size()).isEqualTo(1);
  }

  public SchoolTombstone createMockSchool() {
    final SchoolTombstone schoolTombstone = new SchoolTombstone();
    schoolTombstone.setSchoolId(UUID.randomUUID().toString());
    schoolTombstone.setDistrictId(UUID.randomUUID().toString());
    schoolTombstone.setDisplayName("Marco's school");
    schoolTombstone.setMincode("03636018");
    schoolTombstone.setOpenedDate("1964-09-01T00:00:00");
    schoolTombstone.setSchoolCategoryCode("PUBLIC");
    schoolTombstone.setSchoolReportingRequirementCode("REGULAR");
    schoolTombstone.setFacilityTypeCode("STANDARD");
    return schoolTombstone;
  }

  public ChallengeReportsReportingPeriodEntity createMockReportingPeriod() {
    final ChallengeReportsReportingPeriodEntity reportingPeriod = new ChallengeReportsReportingPeriodEntity();
    reportingPeriod.setSchoolYear("2024");
    reportingPeriod.setActiveFromDate(LocalDateTime.now().minusDays(1));
    reportingPeriod.setActiveToDate(LocalDateTime.now().plusDays(1));
    reportingPeriod.setCreateUser("ABC");
    reportingPeriod.setCreateDate(LocalDateTime.now());
    reportingPeriod.setUpdateUser("ABC");
    reportingPeriod.setUpdateDate(LocalDateTime.now());
    return reportingPeriod;
  }

  public ChallengeReportsSessionEntity createMockSession(ChallengeReportsReportingPeriodEntity reportingPeriod) {
    final ChallengeReportsSessionEntity session = new ChallengeReportsSessionEntity();
    session.setChallengeReportsPeriod(reportingPeriod);
    session.setChallengeReportsStatusCode("PRELIM");
    session.setFundingRate("234");
    session.setFinalDateForChanges(LocalDateTime.now());
    session.setCreateUser("ABC");
    session.setCreateDate(LocalDateTime.now());
    session.setUpdateUser("ABC");
    session.setUpdateDate(LocalDateTime.now());
    return session;
  }
}
