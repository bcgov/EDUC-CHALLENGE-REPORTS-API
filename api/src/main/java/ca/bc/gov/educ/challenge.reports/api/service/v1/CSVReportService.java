package ca.bc.gov.educ.challenge.reports.api.service.v1;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportTypeCode;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.DistrictReportHeader;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.SdcInvalidSchoolFundingCode;
import ca.bc.gov.educ.challenge.reports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challenge.reports.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challenge.reports.api.rest.RestUtils;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsStudentRecord;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.coreg.v1.CourseCode;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.gradstudent.v1.StudentCoursePagination;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.District;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.sdc.v1.SdcSchoolCollectionStudent;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {

    private final ChallengeReportsSessionRepository challengeReportsSessionRepository;
    private final RestUtils restUtils;

    public DownloadableReportResponse generateChallengeReportForThisYear(String districtID) throws JsonProcessingException {
        var currentReportingPeriod = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "reportingPeriodSession", null));

        var currentStage = currentReportingPeriod.getChallengeReportsStatusCode();
        var schoolYear = currentReportingPeriod.getChallengeReportsPeriod().getSchoolYear();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if(currentStage.equalsIgnoreCase(ChallengeReportsStatus.PRELIM.toString())){
            var gradStudents = restUtils.getChallengeReportGradStudentCoursesForYear(getCourseSessionValues(schoolYear));

            Map<String, StudentCoursePagination> gradStudentsMap = gradStudents.stream().collect(Collectors.toMap(studentCourse -> studentCourse.getGradStudent().getStudentID().toString(), item -> item));

            var lastYear = Year.of(Integer.parseInt(schoolYear)).minusYears(1);

            var collectionsPagination = restUtils.getLastSeptemberCollection(lastYear.toString());
            var collection = collectionsPagination.getContent().get(0);

            var sdcStudents = restUtils.get1701DataForStudents(collection.getCollectionID(), gradStudentsMap.keySet().stream().toList());

            var mapOfStudents = new HashMap<String, SdcSchoolCollectionStudent>();
            var fullStudentList = new ArrayList<ChallengeReportsStudentRecord>();
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
                        if(currentSchool.getDistrictId().equalsIgnoreCase(districtID) && currentSchool.getSchoolCategoryCode().equalsIgnoreCase("PUBLIC")) {
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
            byteArrayOutputStream = generateChallengeReportCSVBytes(fullStudentList);
        }else{

        }

        var downloadableReport = new DownloadableReportResponse();

        downloadableReport.setReportType(ChallengeReportTypeCode.DISTRICT_REPORT.getCode());
        downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

        return downloadableReport;
    }

    private ByteArrayOutputStream generateChallengeReportCSVBytes(List<ChallengeReportsStudentRecord> students){
        String[] headers = DistrictReportHeader.getAllValuesAsStringArray();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (ChallengeReportsStudentRecord result : students) {
                var schoolOpt = restUtils.getSchoolBySchoolID(result.getSchoolID().toString());
                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();

                    List<String> csvRowData = prepareStudentDataForCsv(result, school);
                    csvPrinter.printRecord(csvRowData);
                }
            }
            csvPrinter.flush();

            return byteArrayOutputStream;
        } catch (IOException e) {
            throw new ChallengeReportsAPIRuntimeException(e);
        }
    }

    private List<String> prepareStudentDataForCsv(ChallengeReportsStudentRecord student, SchoolTombstone school) {
        return new ArrayList<>(Arrays.asList(
                school.getMincode(),
                school.getDisplayName(),
                student.getCourseSession(),
                student.getCourseCodeAndLevel(),
                student.getPen(),
                student.getStudentSurname(),
                student.getStudentGivenName(),
                student.getStudentMiddleNames()
        ));
    }

    private String getCourseCodeValueFromExternalCode(String externalCode){
        return externalCode.substring(0, 5);
    }

    private String getCourseLevelValueFromExternalCode(String externalCode){
        return externalCode.substring(5);
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
