package ca.bc.gov.educ.challenge.reports.api.service.v1;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.*;
import ca.bc.gov.educ.challenge.reports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challenge.reports.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsPostedStudentEntity;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsPostedStudentRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challenge.reports.api.rest.RestUtils;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsStudentRecord;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.District;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.sdc.v1.Collection;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.sdc.v1.IndependentSchoolFundingGroupSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Year;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {

    private final ChallengeReportsSessionRepository challengeReportsSessionRepository;
    private final ChallengeReportsPostedStudentRepository challengeReportsPostedStudentRepository;
    private final ChallengeReportsService challengeReportsService;
    private final RestUtils restUtils;

    public DownloadableReportResponse generateChallengeReportForThisYear(String districtID) throws JsonProcessingException {
        var currentReportingPeriod = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "reportingPeriodSession", null));

        var finalStudentDistrictList = new ArrayList<ChallengeReportsStudentRecord>();
        var currentStage = currentReportingPeriod.getChallengeReportsStatusCode();
        ByteArrayOutputStream byteArrayOutputStream;

        if (currentStage.equalsIgnoreCase(ChallengeReportsStatus.PRELIM.toString())) {
            var fullStudentList = challengeReportsService.getAndGeneratePreliminaryChallengeStudentList(currentReportingPeriod);
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
        byteArrayOutputStream = generateChallengeReportCSVBytes(finalStudentDistrictList);
        var downloadableReport = new DownloadableReportResponse();

        downloadableReport.setReportType(ChallengeReportTypeCode.DISTRICT_REPORT.getCode());
        downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

        return downloadableReport;
    }

    private ByteArrayOutputStream generateChallengeReportCSVBytes(List<ChallengeReportsStudentRecord> students) {
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
                if (schoolOpt.isPresent()) {
                    var school = schoolOpt.get();

                    List<String> csvRowData = prepareStudentDataForChallengeReportStudentCsv(result, school);
                    csvPrinter.printRecord(csvRowData);
                }
            }
            csvPrinter.flush();

            return byteArrayOutputStream;
        } catch (IOException e) {
            throw new ChallengeReportsAPIRuntimeException(e);
        }
    }

    private List<String> prepareStudentDataForChallengeReportStudentCsv(ChallengeReportsStudentRecord student, SchoolTombstone school) {
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

    private List<String> prepareStudentDataForDistrictFundingReport(District district, String count) {
        return new ArrayList<>(Arrays.asList(
                district.getDistrictNumber(),
                district.getDisplayName(),
                count
        ));
    }

    private List<String> prepareStudentDataForIndependentFundingReport(SchoolTombstone school, ChallengeReportsPostedStudentEntity student, Collection collection) {
        var schoolGroups = restUtils.getSchoolFundingGroupsForCollection(collection.getCollectionID());
        var fundingGroup = "";

        if(StringUtils.isNotBlank(student.getCourseCodeAndLevel())) {
            fundingGroup = getFundingGroupSnapshotForGrade(schoolGroups, student.getCourseCodeAndLevel().replaceAll("[^0-9]", ""));
        }

        return new ArrayList<>(Arrays.asList(
                school.getMincode(),
                school.getDisplayName(),
                fundingGroup,
                student.getPen(),
                student.getCourseCodeAndLevel()
        ));
    }

    private String getFundingGroupSnapshotForGrade(List<IndependentSchoolFundingGroupSnapshot> schoolFundingGroups, String gradeCode) {
        return schoolFundingGroups
                .stream()
                .filter(group -> gradeCode.equals(group.getSchoolGradeCode()))
                .map(IndependentSchoolFundingGroupSnapshot::getSchoolFundingGroupCode)
                .findFirst()
                .orElse("");
    }

    public DownloadableReportResponse generateDistrictFundingReport() {
        var currentReportingPeriod = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "reportingPeriodSession", null));
        var allStudents = challengeReportsPostedStudentRepository.findAllByChallengeReportsSessionEntity_ChallengeReportsSessionID(currentReportingPeriod.getChallengeReportsSessionID());
        String[] headers = MinistryDistrictReportHeader.getAllValuesAsStringArray();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);


            var districtCounts = new HashMap<UUID, Integer>();
            allStudents.forEach(challengeReportsStudentRecord -> {
                var school = restUtils.getSchoolBySchoolID(challengeReportsStudentRecord.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "schoolID", challengeReportsStudentRecord.getSchoolID().toString()));
                if(school.getSchoolCategoryCode().equalsIgnoreCase("PUBLIC")) {
                    if (districtCounts.containsKey(challengeReportsStudentRecord.getDistrictID())) {
                        districtCounts.put(challengeReportsStudentRecord.getDistrictID(), districtCounts.get(challengeReportsStudentRecord.getDistrictID()) + 1);
                    } else {
                        districtCounts.put(challengeReportsStudentRecord.getDistrictID(), 1);
                    }
                }
            });

            for (UUID districtID : districtCounts.keySet()) {
                var district = restUtils.getDistrictByDistrictID(districtID.toString()).orElseThrow(() -> new EntityNotFoundException(District.class, "districtID", districtID.toString()));
                List<String> csvRowData = prepareStudentDataForDistrictFundingReport(district, districtCounts.get(districtID).toString());
                csvPrinter.printRecord(csvRowData);
            }

            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();

            downloadableReport.setReportType(MinistryReportTypeCode.DISTRICT_FUNDING_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new ChallengeReportsAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateIndependentSchoolsFundingReport() throws JsonProcessingException {
        var currentReportingPeriod = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "reportingPeriodSession", null));
        var allStudents = challengeReportsPostedStudentRepository.findAllByChallengeReportsSessionEntity_ChallengeReportsSessionID(currentReportingPeriod.getChallengeReportsSessionID());
        var lastYear = Year.of(Integer.parseInt(currentReportingPeriod.getChallengeReportsPeriod().getSchoolYear())).minusYears(1);
        var collection = restUtils.getLastSeptemberCollection(lastYear.toString());
        String[] headers = MinistryIndependentSchoolsReportHeader.getAllValuesAsStringArray();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);


            var finalStudents = new ArrayList<ChallengeReportsPostedStudentEntity>();
            allStudents.forEach(challengeReportsStudentRecord -> {
                var school = restUtils.getSchoolBySchoolID(challengeReportsStudentRecord.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "schoolID", challengeReportsStudentRecord.getSchoolID().toString()));
                if(school.getSchoolCategoryCode().equalsIgnoreCase("INDEPEND") || school.getSchoolCategoryCode().equalsIgnoreCase("INDP_FNS")) {
                    finalStudents.add(challengeReportsStudentRecord);
                }
            });

            for (var student : finalStudents) {
                var school = restUtils.getSchoolBySchoolID(student.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "schoolID", student.getSchoolID().toString()));
                List<String> csvRowData = prepareStudentDataForIndependentFundingReport(school, student, collection.getContent().get(0));
                csvPrinter.printRecord(csvRowData);
            }

            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();

            downloadableReport.setReportType(MinistryReportTypeCode.DISTRICT_FUNDING_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new ChallengeReportsAPIRuntimeException(e);
        }
    }

}
