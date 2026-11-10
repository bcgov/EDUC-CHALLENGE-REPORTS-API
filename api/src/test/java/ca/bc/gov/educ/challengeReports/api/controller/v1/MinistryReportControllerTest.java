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
import ca.bc.gov.educ.challengeReports.api.struct.v1.DistrictChallengeReportsCounts;
import ca.bc.gov.educ.challengeReports.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.PaginatedResponse;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.coreg.v1.CourseCode;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.gradstudent.v1.GraduationStudentPaginationRecord;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.gradstudent.v1.StudentCoursePagination;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.institute.v1.District;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.Collection;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.IndependentSchoolFundingGroupSnapshot;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.SdcSchoolCollection;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.SdcSchoolCollectionStudent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
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
  void setUp() {
    reset(restUtils);
    challengeReportsPostedStudentRepository.deleteAll();
    challengeReportsSessionRepository.deleteAll();
    challengeReportsReportingPeriodRepository.deleteAll();
  }

  @Test
  void testPrelimIndependentReport_whilePrelim_usesReportDateAndGroupFromEachStudentsCollection() throws Exception {
    createSession("PRELIM");

    var school = createMockSchool("INDEPEND");
    var septStudentID = UUID.randomUUID();
    var febStudentID = UUID.randomUUID();
    var septCollectionID = UUID.randomUUID().toString();
    var febCollectionID = UUID.randomUUID().toString();

    when(restUtils.getChallengeReportGradStudentCoursesForYear(anyList())).thenReturn(List.of(
            createStudentCourse(septStudentID, school.getSchoolId()),
            createStudentCourse(febStudentID, school.getSchoolId())));

    var septCollection = new Collection();
    septCollection.setCollectionID(septCollectionID);
    var febCollection = new Collection();
    febCollection.setCollectionID(febCollectionID);
    when(restUtils.getLastSeptemberCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(septCollection)));
    when(restUtils.getLastFebruaryCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(febCollection)));

    when(restUtils.get1701DataForStudents(eq(septCollectionID), anyList())).thenReturn(List.of(createShallowSdcStudent(septStudentID, school, "111111111", "16")));
    when(restUtils.get1701DataForStudents(eq(febCollectionID), anyList())).thenReturn(List.of(createShallowSdcStudent(febStudentID, school, "222222222", null)));

    when(restUtils.getSdcSchoolCollectionsForCollection(septCollectionID)).thenReturn(List.of(createSdcSchoolCollection(septCollectionID, school, "20240929")));
    when(restUtils.getSdcSchoolCollectionsForCollection(febCollectionID)).thenReturn(List.of(createSdcSchoolCollection(febCollectionID, school, "20250214")));

    when(restUtils.getSchoolFundingGroupsForCollection(septCollectionID)).thenReturn(List.of(createFundingGroup(school, "GROUP1")));
    when(restUtils.getSchoolFundingGroupsForCollection(febCollectionID)).thenReturn(List.of(createFundingGroup(school, "GROUP2")));

    when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var course = new CourseCode();
    course.setCourseID("MATH12");
    course.setExternalCode("MATH 12");
    when(restUtils.getCoregCourseByID(anyString())).thenReturn(Optional.of(course));

    assertThat(downloadCsv("PRELIM_PROVINCIAL_INDEPENDENT_REPORT")).containsExactly(
            HEADER,
            "03636018,Marco's school,GROUP1,111111111,20240929,16,MATH,12,202407",
            "03636018,Marco's school,GROUP2,222222222,20250214,,MATH,12,202407");
  }

  @Test
  void testPrelimDistrictReport_whenSchoolHasDuplicateSchoolCollections_usesTheOneWithAReportDate() throws Exception {
    createSession("PRELIM");

    var school = createMockSchool("PUBLIC");
    var studentID = UUID.randomUUID();
    var septCollectionID = UUID.randomUUID().toString();
    var febCollectionID = UUID.randomUUID().toString();

    when(restUtils.getChallengeReportGradStudentCoursesForYear(anyList())).thenReturn(List.of(createStudentCourse(studentID, school.getSchoolId())));

    var septCollection = new Collection();
    septCollection.setCollectionID(septCollectionID);
    var febCollection = new Collection();
    febCollection.setCollectionID(febCollectionID);
    when(restUtils.getLastSeptemberCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(septCollection)));
    when(restUtils.getLastFebruaryCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(febCollection)));

    when(restUtils.get1701DataForStudents(eq(septCollectionID), anyList())).thenReturn(List.of(createShallowSdcStudent(studentID, school, "111111111", null)));
    when(restUtils.getSdcSchoolCollectionsForCollection(septCollectionID)).thenReturn(List.of(
            createSdcSchoolCollection(septCollectionID, school, null),
            createSdcSchoolCollection(septCollectionID, school, "20240929")));

    when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var course = new CourseCode();
    course.setCourseID("MATH12");
    course.setExternalCode("MATH 12");
    when(restUtils.getCoregCourseByID(anyString())).thenReturn(Optional.of(course));

    assertThat(downloadCsv("PRELIM_PROVINCIAL_DISTRICT_REPORT")).containsExactly(
            HEADER,
            "03636018,Marco's school,,111111111,20240929,,MATH,12,202407");
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
  void testDistrictFundingReport_countsOnlyFinalRows() throws Exception {
    var session = createSession("FINALIZED");
    var school = createMockSchool("PUBLIC");
    when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var district = new District();
    district.setDistrictId(school.getDistrictId());
    district.setDistrictNumber("036");
    district.setDisplayName("Surrey");
    when(restUtils.getDistrictByDistrictID(school.getDistrictId())).thenReturn(Optional.of(district));

    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "FINAL", "111111111"));
    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "FINAL", "222222222"));
    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "PRELIM", "333333333"));

    assertThat(downloadCsv("DISTRICT_FUNDING_REPORT")).containsExactly(
            "District Number,District Name,Total Successful Course Challenges",
            "036,Surrey,2");
  }

  @Test
  void testIndependentSchoolFundingReport_readsOnlyFinalRowsWithFundingGroup() throws Exception {
    var session = createSession("FINALIZED");
    var school = createMockSchool("INDP_FNS");
    when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var septCollection = new Collection();
    septCollection.setCollectionID(UUID.randomUUID().toString());
    when(restUtils.getLastSeptemberCollection(anyString())).thenReturn(new PaginatedResponse<>(List.of(septCollection)));
    var fundingGroup = new IndependentSchoolFundingGroupSnapshot();
    fundingGroup.setSchoolID(school.getSchoolId());
    fundingGroup.setSchoolGradeCode("GRADE12");
    fundingGroup.setSchoolFundingGroupCode("GROUP1");
    when(restUtils.getSchoolFundingGroupsForCollection(septCollection.getCollectionID())).thenReturn(List.of(fundingGroup));

    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "FINAL", "111111111"));
    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "PRELIM", "333333333"));

    assertThat(downloadCsv("INDEPENDENT_SCHOOL_FUNDING_REPORT")).containsExactly(
            "Mincode,School Name,Funding Group,PEN,Course Code and Level",
            "03636018,Marco's school,GROUP1,111111111,MATH 12");
  }

  @Test
  void testEdxDistrictReportAndCounts_whenFinalized_readOnlyFinalRows() throws Exception {
    var session = createSession("FINALIZED");
    var school = createMockSchool("PUBLIC");
    when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "FINAL", "111111111"));
    challengeReportsPostedStudentRepository.save(createPostedStudent(session, school, "PRELIM", "333333333"));

    var download = this.mockMvc.perform(get(URL.BASE_URL + "/district/" + school.getDistrictId() + "/download").with(mockAuthority)).andExpect(status().isOk()).andReturn();
    var report = objectMapper.readValue(download.getResponse().getContentAsByteArray(), DownloadableReportResponse.class);
    var csv = new String(Base64.getDecoder().decode(report.getDocumentData()), StandardCharsets.UTF_8);
    assertThat(csv).contains("111111111").doesNotContain("333333333");

    var counts = this.mockMvc.perform(get(URL.BASE_URL + "/district/" + school.getDistrictId()).with(mockAuthority)).andExpect(status().isOk()).andReturn();
    var districtCounts = objectMapper.readValue(counts.getResponse().getContentAsByteArray(), DistrictChallengeReportsCounts.class);
    assertThat(districtCounts.getSchoolsWithCounts()).singleElement().satisfies(count -> assertThat(count.getCount()).isEqualTo("1"));
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
  void testSavePostedStudents_whenCalledConcurrently_savesOnlyOneSnapshot() throws Exception {
    var session = createSession("PRELIM");
    var school = createMockSchool("PUBLIC");
    var records = new ArrayList<ChallengeReportsStudentRecord>();
    for (int i = 0; i < 200; i++) {
      var studentRecord = new ChallengeReportsStudentRecord();
      studentRecord.setSchoolID(UUID.fromString(school.getSchoolId()));
      studentRecord.setDistrictID(UUID.fromString(school.getDistrictId()));
      studentRecord.setStudentID(UUID.randomUUID());
      studentRecord.setPen(String.format("%09d", i));
      studentRecord.setCourseSession("202407");
      studentRecord.setCourseCodeAndLevel("MATH 12");
      studentRecord.setStudentSurname("SMITH");
      records.add(studentRecord);
    }

    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var saves = List.of(
              executor.submit(() -> { start.await(); challengeReportsService.savePostedStudentsIfAbsent(records, session, "ABC", ChallengeReportsStage.PRELIM); return null; }),
              executor.submit(() -> { start.await(); challengeReportsService.savePostedStudentsIfAbsent(records, session, "ABC", ChallengeReportsStage.PRELIM); return null; }));
      start.countDown();
      for (var save : saves) {
        save.get(30, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
    }

    assertThat(challengeReportsPostedStudentRepository.findAllByChallengeReportsSessionEntity_ChallengeReportsSessionIDAndStageCode(session.getChallengeReportsSessionID(), "PRELIM")).hasSize(200);
  }

  @Test
  void testProvincialReport_splitsFixedWidthCourseCodeAndLevel() throws Exception {
    var session = createSession("FINALIZED");
    var school = createMockSchool("PUBLIC");
    when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    var paddedShortCode = createPostedStudent(session, school, "FINAL", "111111111");
    paddedShortCode.setCourseCodeAndLevel("CLE  10");
    challengeReportsPostedStudentRepository.save(paddedShortCode);
    var codeWithoutLevel = createPostedStudent(session, school, "FINAL", "222222222");
    codeWithoutLevel.setCourseCodeAndLevel("LTE");
    challengeReportsPostedStudentRepository.save(codeWithoutLevel);

    assertThat(downloadCsv("FINAL_PROVINCIAL_DISTRICT_REPORT")).containsExactly(
            HEADER,
            "03636018,Marco's school,,111111111,20240929,,CLE,10,202407",
            "03636018,Marco's school,,222222222,20240929,,LTE,,202407");
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

  private SdcSchoolCollectionStudent createShallowSdcStudent(UUID studentID, SchoolTombstone school, String pen, String fundingCode) {
    var sdcStudent = new SdcSchoolCollectionStudent();
    sdcStudent.setSchoolID(school.getSchoolId());
    sdcStudent.setAssignedStudentId(studentID.toString());
    sdcStudent.setAssignedPen(pen);
    sdcStudent.setLegalLastName("SMITH");
    sdcStudent.setSchoolFundingCode(fundingCode);
    sdcStudent.setFte(BigDecimal.ONE);
    return sdcStudent;
  }

  private SdcSchoolCollection createSdcSchoolCollection(String collectionID, SchoolTombstone school, String uploadReportDate) {
    var sdcSchoolCollection = new SdcSchoolCollection();
    sdcSchoolCollection.setSdcSchoolCollectionID(UUID.randomUUID().toString());
    sdcSchoolCollection.setCollectionID(collectionID);
    sdcSchoolCollection.setSchoolID(school.getSchoolId());
    sdcSchoolCollection.setUploadReportDate(uploadReportDate);
    return sdcSchoolCollection;
  }

  private IndependentSchoolFundingGroupSnapshot createFundingGroup(SchoolTombstone school, String fundingGroupCode) {
    var fundingGroup = new IndependentSchoolFundingGroupSnapshot();
    fundingGroup.setSchoolID(school.getSchoolId());
    fundingGroup.setSchoolGradeCode("GRADE12");
    fundingGroup.setSchoolFundingGroupCode(fundingGroupCode);
    return fundingGroup;
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
