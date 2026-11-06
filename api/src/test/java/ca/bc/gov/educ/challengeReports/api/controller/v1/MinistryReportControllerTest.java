package ca.bc.gov.educ.challengeReports.api.controller.v1;

import ca.bc.gov.educ.challengeReports.api.BaseChallengeReportsAPITest;
import ca.bc.gov.educ.challengeReports.api.constants.v1.ChallengeReportsStage;
import ca.bc.gov.educ.challengeReports.api.constants.v1.URL;
import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsPostedStudentEntity;
import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsReportingPeriodEntity;
import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challengeReports.api.repository.v1.ChallengeReportsPostedStudentRepository;
import ca.bc.gov.educ.challengeReports.api.repository.v1.ChallengeReportsReportingPeriodRepository;
import ca.bc.gov.educ.challengeReports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challengeReports.api.rest.RestUtils;
import ca.bc.gov.educ.challengeReports.api.service.v1.ChallengeReportsService;
import ca.bc.gov.educ.challengeReports.api.struct.v1.ChallengeReportsStudentRecord;
import ca.bc.gov.educ.challengeReports.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.PaginatedResponse;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.coreg.v1.CourseCode;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.gradstudent.v1.GraduationStudentPaginationRecord;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.gradstudent.v1.StudentCoursePagination;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.Collection;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.IndependentSchoolFundingGroupSnapshot;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.SdcSchoolCollection;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.SdcSchoolCollectionStudent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MinistryReportControllerTest extends BaseChallengeReportsAPITest {

  private static final String HEADER = "MINCODE,SCHOOL NAME,GROUP,PEN,REPORT DATE,FUNDING CODE,COURSE CODE,COURSE LEVEL,COURSE SESSION";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  ChallengeReportsReportingPeriodRepository challengeReportsReportingPeriodRepository;
  @Autowired
  ChallengeReportsSessionRepository challengeReportsSessionRepository;
  @Autowired
  ChallengeReportsPostedStudentRepository challengeReportsPostedStudentRepository;
  @Autowired
  RestUtils restUtils;
  @Autowired
  ChallengeReportsService challengeReportsService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities((GrantedAuthority) () -> "SCOPE_READ_CHALLENGE_REPORTS");

  @BeforeEach
  public void setUp() {
    Mockito.reset(restUtils);
    challengeReportsPostedStudentRepository.deleteAll();
    challengeReportsSessionRepository.deleteAll();
    challengeReportsReportingPeriodRepository.deleteAll();
  }

  @Test
  void testPrelimIndependentReport_whilePrelim_isGeneratedLive() throws Exception {
    createSession("PRELIM");

    var school = createMockSchool("INDEPEND");
    var studentID = UUID.randomUUID();
    var septCollectionID = UUID.randomUUID().toString();
    var sdcSchoolCollectionID = UUID.randomUUID().toString();

    when(restUtils.getChallengeReportGradStudentCoursesForYear(anyList(), any())).thenReturn(List.of(createStudentCourse(studentID, school.getSchoolId())));

    var septCollection = new Collection();
    septCollection.setCollectionID(septCollectionID);
    var febCollection = new Collection();
    febCollection.setCollectionID(UUID.randomUUID().toString());
    when(restUtils.getLastSeptemberCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(septCollection)));
    when(restUtils.getLastFebruaryCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(febCollection)));

    var sdcStudent = new SdcSchoolCollectionStudent();
    sdcStudent.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcStudent.setSchoolID(school.getSchoolId());
    sdcStudent.setAssignedStudentId(studentID.toString());
    sdcStudent.setAssignedPen("123456789");
    sdcStudent.setLegalLastName("SMITH");
    sdcStudent.setSchoolFundingCode("16");
    sdcStudent.setFte(BigDecimal.ONE);
    when(restUtils.get1701DataForStudents(eq(septCollectionID), anyList())).thenReturn(List.of(sdcStudent));

    var sdcSchoolCollection = new SdcSchoolCollection();
    sdcSchoolCollection.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollection.setCollectionID(septCollectionID);
    sdcSchoolCollection.setSchoolID(school.getSchoolId());
    sdcSchoolCollection.setUploadReportDate("20240929");
    when(restUtils.getSdcSchoolCollectionsForCollection(septCollectionID)).thenReturn(List.of(sdcSchoolCollection));

    var fundingGroup = new IndependentSchoolFundingGroupSnapshot();
    fundingGroup.setSchoolID(school.getSchoolId());
    fundingGroup.setSchoolGradeCode("GRADE12");
    fundingGroup.setSchoolFundingGroupCode("GROUP1");
    when(restUtils.getSchoolFundingGroupsForCollection(septCollectionID)).thenReturn(List.of(fundingGroup));

    when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var course = new CourseCode();
    course.setCourseID("MATH12");
    course.setExternalCode("MATH 12");
    when(restUtils.getCoregCourseByID(anyString())).thenReturn(Optional.of(course));

    var csv = downloadCsv("PRELIM_PROVINCIAL_INDEPENDENT_REPORT");

    assertThat(csv).containsExactly(HEADER, "03636018,Marco's school,GROUP1,123456789,20240929,16,MATH,12,202407");
  }

  @Test
  void testFinalDistrictReport_readsOnlyFinalRows() throws Exception {
    var session = createSession("FINALIZED");
    var school = createMockSchool("PUBLIC");
    when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "FINAL", "111111111"));
    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "PRELIM", "222222222"));

    assertThat(downloadCsv("FINAL_PROVINCIAL_DISTRICT_REPORT")).containsExactly(HEADER, "03636018,Marco's school,,111111111,20240929,,MATH,12,202407");
    assertThat(downloadCsv("PRELIM_PROVINCIAL_DISTRICT_REPORT")).containsExactly(HEADER, "03636018,Marco's school,,222222222,20240929,,MATH,12,202407");
    assertThat(downloadCsv("FINAL_PROVINCIAL_INDEPENDENT_REPORT")).containsExactly(HEADER);
  }

  @Test
  void testFinalDistrictReport_isSortedByMincodePenSessionAndCourse() throws Exception {
    var session = createSession("FINALIZED");
    var schoolA = createMockSchool("PUBLIC");
    var schoolB = createMockSchool("PUBLIC");
    schoolB.setMincode("01111111");
    when(restUtils.getSchoolBySchoolID(schoolA.getSchoolId())).thenReturn(Optional.of(schoolA));
    when(restUtils.getSchoolBySchoolID(schoolB.getSchoolId())).thenReturn(Optional.of(schoolB));

    var later = createPostedStudent(session, schoolA, "FINAL", "111111111");
    later.setCourseSession("202501");
    challengeReportsPostedStudentRepository.save(later);
    var physics = createPostedStudent(session, schoolA, "FINAL", "111111111");
    physics.setCourseCodeAndLevel("PHYS 12");
    challengeReportsPostedStudentRepository.save(physics);
    challengeReportsPostedStudentRepository.save(createPostedStudent(session, schoolA, "FINAL", "222222222"));
    challengeReportsPostedStudentRepository.save(createPostedStudent(session, schoolA, "FINAL", "111111111"));
    challengeReportsPostedStudentRepository.save(createPostedStudent(session, schoolB, "FINAL", "999999999"));

    assertThat(downloadCsv("FINAL_PROVINCIAL_DISTRICT_REPORT")).containsExactly(
            HEADER,
            "01111111,Marco's school,,999999999,20240929,,MATH,12,202407",
            "03636018,Marco's school,,111111111,20240929,,MATH,12,202407",
            "03636018,Marco's school,,111111111,20240929,,PHYS,12,202407",
            "03636018,Marco's school,,111111111,20240929,,MATH,12,202501",
            "03636018,Marco's school,,222222222,20240929,,MATH,12,202407");
  }

  @Test
  void testSavePostedStudents_whenStageAlreadySaved_leavesExistingRowsUntouched() {
    var session = createSession("FINALIZED");
    var school = createMockSchool("PUBLIC");
    var original = challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "FINAL", "111111111"));

    var retryRecord = new ChallengeReportsStudentRecord();
    retryRecord.setSchoolID(UUID.fromString(school.getSchoolId()));
    retryRecord.setDistrictID(UUID.fromString(school.getDistrictId()));
    retryRecord.setStudentID(UUID.randomUUID());
    retryRecord.setPen("999999999");
    retryRecord.setCourseSession("202407");
    retryRecord.setCourseCodeAndLevel("MATH 12");
    retryRecord.setStudentSurname("JONES");

    challengeReportsService.savePostedStudentsIfAbsent(List.of(retryRecord), session, "ABC", ChallengeReportsStage.FINAL);
    challengeReportsService.savePostedStudentsIfAbsent(List.of(retryRecord), session, "ABC", ChallengeReportsStage.PRELIM);

    var finalRows = challengeReportsPostedStudentRepository.findAllByChallengeReportsSessionEntity_ChallengeReportsSessionIDAndStageCode(session.getChallengeReportsSessionID(), "FINAL");
    var prelimRows = challengeReportsPostedStudentRepository.findAllByChallengeReportsSessionEntity_ChallengeReportsSessionIDAndStageCode(session.getChallengeReportsSessionID(), "PRELIM");
    assertThat(finalRows).extracting(ChallengeReportsPostedStudentEntity::getChallengeReportsPostedStudentID).containsExactly(original.getChallengeReportsPostedStudentID());
    assertThat(prelimRows).extracting(ChallengeReportsPostedStudentEntity::getPen).containsExactly("999999999");
  }

  @Test
  void testInvalidReportType_returnsBadRequest() throws Exception {
    this.mockMvc.perform(get(URL.BASE_URL + "/report/NOT_A_REPORT/download").with(mockAuthority)).andExpect(status().isBadRequest());
  }

  private List<String> downloadCsv(String reportType) throws Exception {
    var result = this.mockMvc.perform(get(URL.BASE_URL + "/report/" + reportType + "/download").with(mockAuthority)).andExpect(status().isOk()).andReturn();
    var response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), DownloadableReportResponse.class);
    assertThat(response.getReportType()).isEqualTo(reportType);
    return new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8).lines().toList();
  }

  private ChallengeReportsSessionEntity createSession(String status) {
    var reportingPeriod = new ChallengeReportsReportingPeriodEntity();
    reportingPeriod.setSchoolYear("2025");
    reportingPeriod.setActiveFromDate(LocalDateTime.now().minusDays(1));
    reportingPeriod.setActiveToDate(LocalDateTime.now().plusDays(1));
    reportingPeriod.setCreateUser("ABC");
    reportingPeriod.setCreateDate(LocalDateTime.now());
    reportingPeriod.setUpdateUser("ABC");
    reportingPeriod.setUpdateDate(LocalDateTime.now());
    reportingPeriod = challengeReportsReportingPeriodRepository.save(reportingPeriod);

    var session = new ChallengeReportsSessionEntity();
    session.setChallengeReportsPeriod(reportingPeriod);
    session.setChallengeReportsStatusCode(status);
    session.setFundingRate("234");
    session.setFinalDateForChanges(LocalDateTime.now());
    session.setCreateUser("ABC");
    session.setCreateDate(LocalDateTime.now());
    session.setUpdateUser("ABC");
    session.setUpdateDate(LocalDateTime.now());
    return challengeReportsSessionRepository.save(session);
  }

  private StudentCoursePagination createStudentCourse(UUID studentID, String schoolID) {
    var gradStudent = new GraduationStudentPaginationRecord();
    gradStudent.setPen("123456789");
    gradStudent.setSchoolOfRecordId(UUID.fromString(schoolID));
    gradStudent.setStudentID(studentID);

    var studentCourse = new StudentCoursePagination();
    studentCourse.setCourseID("MATH12");
    studentCourse.setEquivOrChallenge("C");
    studentCourse.setGradStudent(gradStudent);
    studentCourse.setFinalPercent(66);
    studentCourse.setStudentCourseID(UUID.randomUUID());
    studentCourse.setCourseSession("202407");
    return studentCourse;
  }

  private ChallengeReportsPostedStudentEntity createPostedStudent(ChallengeReportsSessionEntity session, SchoolTombstone school, String stageCode, String pen) {
    return ChallengeReportsPostedStudentEntity.builder()
            .challengeReportsSessionEntity(session)
            .schoolID(UUID.fromString(school.getSchoolId()))
            .districtID(UUID.fromString(school.getDistrictId()))
            .studentID(UUID.randomUUID())
            .pen(pen)
            .courseSession("202407")
            .courseCodeAndLevel("MATH 12")
            .studentSurname("SMITH")
            .stageCode(stageCode)
            .reportDate("20240929")
            .createUser("ABC")
            .createDate(LocalDateTime.now())
            .updateUser("ABC")
            .updateDate(LocalDateTime.now())
            .build();
  }

  private SchoolTombstone createMockSchool(String category) {
    var school = new SchoolTombstone();
    school.setSchoolId(UUID.randomUUID().toString());
    school.setDistrictId(UUID.randomUUID().toString());
    school.setDisplayName("Marco's school");
    school.setMincode("03636018");
    school.setSchoolCategoryCode(category);
    return school;
  }
}
