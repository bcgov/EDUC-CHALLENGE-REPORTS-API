package ca.bc.gov.educ.challenge.reports.api.service.v1;

import ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.SdcInvalidSchoolFundingCode;
import ca.bc.gov.educ.challenge.reports.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsPostedStudentEntity;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsPostedStudentRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challenge.reports.api.rest.RestUtils;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsStudentRecord;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.HasChallengeReportsStudentsResponse;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.coreg.v1.CourseCode;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.gradstudent.v1.StudentCoursePagination;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.sdc.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.challenge.reports.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus.PRELIM;

@RequiredArgsConstructor
@Service
public class ChallengeReportsService {
    private final ChallengeReportsSessionRepository challengeReportsSessionRepository;
    private final ChallengeReportsPostedStudentRepository challengeReportsPostedStudentRepository;
    private final RestUtils restUtils;

    public ChallengeReportsSessionEntity getChallengeReportActiveSession() {
        return challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "challengeReportsSession", "activeSession"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChallengeReportsSessionEntity updateChallengeReportsSessionAttributes(ChallengeReportsSessionEntity challengeReportsSessionEntity) {
        var currentSession = this.challengeReportsSessionRepository.findById(challengeReportsSessionEntity.getChallengeReportsSessionID()).orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "challengeReportsSession", challengeReportsSessionEntity.getChallengeReportsSessionID().toString()));
        BeanUtils.copyProperties(challengeReportsSessionEntity, currentSession, "challengeReportsSessionID", "challengeReportsStatusCode");
        TransformUtil.uppercaseFields(currentSession);
        return this.challengeReportsSessionRepository.save(currentSession);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAllPostedStudents(List<ChallengeReportsStudentRecord> challengeReportsStudentRecords, ChallengeReportsSessionEntity challengeReportsSessionEntity, String updateUser) {
        var postedStudents = new ArrayList<ChallengeReportsPostedStudentEntity>();
        challengeReportsStudentRecords.forEach(student -> {
            ChallengeReportsPostedStudentEntity postedStudent = new ChallengeReportsPostedStudentEntity();
            postedStudent.setChallengeReportsSessionEntity(challengeReportsSessionEntity);
            postedStudent.setSchoolID(student.getSchoolID());
            postedStudent.setDistrictID(student.getDistrictID());
            postedStudent.setStudentID(student.getStudentID());
            postedStudent.setPen(student.getPen());
            postedStudent.setCourseSession(student.getCourseSession());
            postedStudent.setCourseCodeAndLevel(student.getCourseCodeAndLevel());
            postedStudent.setStudentSurname(student.getStudentSurname());
            postedStudent.setStudentGivenName(student.getStudentGivenName());
            postedStudent.setStudentMiddleNames(student.getStudentMiddleNames());
            postedStudent.setCreateUser(updateUser);
            postedStudent.setCreateDate(LocalDateTime.now());
            postedStudent.setUpdateUser(updateUser);
            postedStudent.setUpdateDate(LocalDateTime.now());
            postedStudents.add(postedStudent);
        });
        this.challengeReportsPostedStudentRepository.saveAll(postedStudents);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateChallengeReportsStatus(ChallengeReportsStatus status, String updateUser) {
        var currentSession = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "challengeReportsSession", "activeSession"));
        currentSession.setUpdateDate(LocalDateTime.now());
        currentSession.setUpdateUser(updateUser);
        currentSession.setChallengeReportsStatusCode(status.toString());

        if (status.equals(PRELIM)) {
            currentSession.setPreliminaryStageCompletionDate(LocalDateTime.now());
        } else {
            currentSession.setFinalStageCompletionDate(LocalDateTime.now());
        }

        this.challengeReportsSessionRepository.save(currentSession);
    }

    public List<ChallengeReportsStudentRecord> getAndGeneratePreliminaryChallengeStudentList(ChallengeReportsSessionEntity challengeReportsSession) throws JsonProcessingException {
        var fullStudentList = new ArrayList<ChallengeReportsStudentRecord>();
        var schoolYear = challengeReportsSession.getChallengeReportsPeriod().getSchoolYear();
        var gradStudents = restUtils.getChallengeReportGradStudentCoursesForYear(getCourseSessionValues(schoolYear));

        Map<String, StudentCoursePagination> gradStudentsMap = gradStudents.stream().collect(Collectors.toMap(studentCourse -> studentCourse.getGradStudent().getStudentID().toString(), item -> item));

        var lastYear = Year.of(Integer.parseInt(schoolYear)).minusYears(1);

        var collectionsPagination = restUtils.getLastSeptemberCollection(lastYear.toString());
        var collection = collectionsPagination.getContent().get(0);

        var sdcStudents = restUtils.get1701DataForStudents(collection.getCollectionID(), gradStudentsMap.keySet().stream().toList());

        var mapOfStudents = new HashMap<String, SdcSchoolCollectionStudent>();
        sdcStudents.forEach(sdcSchoolCollectionStudent -> {
            var school = restUtils.getSchoolBySchoolID(sdcSchoolCollectionStudent.getSchoolID()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "school", sdcSchoolCollectionStudent.getSchoolID()));
            if(Arrays.stream(SdcInvalidSchoolFundingCode.getSdcInvalidSchoolFundingCode()).noneMatch(val -> val.equals(sdcSchoolCollectionStudent.getSchoolFundingCode()))) {
                //Maybe ween out district students here && districtID.equals(UUID.fromString(school.getDistrictId())) -> also public only

                //Valid student in this district
                if(mapOfStudents.containsKey(sdcSchoolCollectionStudent.getAssignedStudentId())) {
                    //Already contains - pull the record which was in the map - compare against incoming - FTE wins - if FTE ties - lowest mincode wins
                    var existingStudentInMap = mapOfStudents.get(sdcSchoolCollectionStudent.getAssignedStudentId());

                    if(existingStudentInMap.getFte().compareTo(sdcSchoolCollectionStudent.getFte()) < 0){
                        mapOfStudents.put(sdcSchoolCollectionStudent.getAssignedStudentId(), sdcSchoolCollectionStudent);
                    }else if(existingStudentInMap.getFte().compareTo(sdcSchoolCollectionStudent.getFte()) == 0){
                        var currentSchool = restUtils.getSchoolBySchoolID(existingStudentInMap.getSchoolID()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "school", existingStudentInMap.getSchoolID()));
                        var incomingSchool = restUtils.getSchoolBySchoolID(sdcSchoolCollectionStudent.getSchoolID()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "school", sdcSchoolCollectionStudent.getSchoolID()));

                        if(Integer.parseInt(currentSchool.getMincode()) > Integer.parseInt(incomingSchool.getMincode())) {
                            mapOfStudents.put(sdcSchoolCollectionStudent.getAssignedStudentId(), sdcSchoolCollectionStudent);
                        }
                    }
                }else{
                    mapOfStudents.put(sdcSchoolCollectionStudent.getAssignedStudentId(), sdcSchoolCollectionStudent);
                }

                mapOfStudents.values().forEach(student -> {
                    var currentSchool = restUtils.getSchoolBySchoolID(student.getSchoolID()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "school", student.getSchoolID()));
                    if(currentSchool.getSchoolCategoryCode().equalsIgnoreCase("PUBLIC")) {
                        var gradStudentCourse = gradStudentsMap.get(sdcSchoolCollectionStudent.getAssignedStudentId());
                        var studentRecord = new ChallengeReportsStudentRecord();

                        var coregCourse = restUtils.getCoregCourseByID(gradStudentCourse.getCourseID()).orElseThrow(() -> new EntityNotFoundException(CourseCode.class, "coregCourse", gradStudentCourse.getCourseID()));

                        studentRecord.setSchoolID(UUID.fromString(school.getSchoolId()));
                        studentRecord.setDistrictID(UUID.fromString(school.getDistrictId()));
                        studentRecord.setStudentID(UUID.fromString(sdcSchoolCollectionStudent.getAssignedStudentId()));
                        studentRecord.setPen(sdcSchoolCollectionStudent.getAssignedPen());
                        studentRecord.setCourseSession(gradStudentCourse.getCourseSession());
                        studentRecord.setCourseCodeAndLevel(coregCourse.getExternalCode());
                        studentRecord.setStudentSurname(sdcSchoolCollectionStudent.getLegalLastName());
                        studentRecord.setStudentGivenName(sdcSchoolCollectionStudent.getLegalFirstName());
                        studentRecord.setStudentMiddleNames(sdcSchoolCollectionStudent.getLegalMiddleNames());
                        fullStudentList.add(studentRecord);
                    }
                });
            }
        });
        return fullStudentList;
    }

    public HasChallengeReportsStudentsResponse getHasChallengeReportStudents(String districtID) throws JsonProcessingException {
        var currentReportingPeriod = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "reportingPeriodSession", null));

        var finalStudentDistrictList = new ArrayList<ChallengeReportsStudentRecord>();
        var currentStage = currentReportingPeriod.getChallengeReportsStatusCode();
        ByteArrayOutputStream byteArrayOutputStream;

        if (currentStage.equalsIgnoreCase(ChallengeReportsStatus.PRELIM.toString())) {
            var fullStudentList = getAndGeneratePreliminaryChallengeStudentList(currentReportingPeriod);
            fullStudentList.forEach(student -> {
                if(student.getDistrictID().toString().equalsIgnoreCase(districtID)) {
                    finalStudentDistrictList.add(student);
                }
            });
        } else {
            var postedStudents = challengeReportsPostedStudentRepository.findAllByChallengeReportsSessionEntity_ChallengeReportsSessionID(currentReportingPeriod.getChallengeReportsSessionID());
            postedStudents.forEach(student -> {
                var studentRecord = new ChallengeReportsStudentRecord();

                if(student.getDistrictID().toString().equalsIgnoreCase(districtID)) {
                    studentRecord.setSchoolID(student.getSchoolID());
                    studentRecord.setDistrictID(student.getDistrictID());
                    studentRecord.setStudentID(student.getStudentID());
                    studentRecord.setPen(student.getPen());
                    studentRecord.setCourseSession(student.getCourseSession());
                    studentRecord.setCourseCodeAndLevel(student.getCourseCodeAndLevel());
                    studentRecord.setStudentSurname(student.getStudentSurname());
                    studentRecord.setStudentGivenName(student.getStudentGivenName());
                    studentRecord.setStudentMiddleNames(student.getStudentMiddleNames());
                    finalStudentDistrictList.add(studentRecord);
                }
            });
        }

        var response = new HasChallengeReportsStudentsResponse();
        response.setDistrictID(districtID);
        response.setHasChallengeReportStudents(finalStudentDistrictList.isEmpty() ? "false" : "true");
        return response;
    }

    private List<String> getCourseSessionValues(String year){
        Year previousYear = Year.of(Integer.parseInt(year)).minusYears(1);
        Year currentYear = previousYear.plusYears(1);

        var curYearString = previousYear.toString();
        var nextYearString = currentYear.toString();

        return Arrays.asList(curYearString + "07",
                curYearString + "08",
                curYearString + "09",
                curYearString + "10",
                curYearString + "11",
                curYearString + "12",
                nextYearString + "01",
                nextYearString + "02",
                nextYearString + "03",
                nextYearString + "04",
                nextYearString + "05",
                nextYearString + "06");
    }

}
