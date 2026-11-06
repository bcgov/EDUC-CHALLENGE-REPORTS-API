package ca.bc.gov.educ.challengeReports.api.service.v1;


import ca.bc.gov.educ.challengeReports.api.constants.v1.*;
import ca.bc.gov.educ.challengeReports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challengeReports.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challengeReports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challengeReports.api.rest.RestUtils;
import ca.bc.gov.educ.challengeReports.api.struct.v1.ChallengeReportsStudentRecord;
import ca.bc.gov.educ.challengeReports.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.institute.v1.District;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1.IndependentSchoolFundingGroupSnapshot;
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

    private record SchoolStudent(SchoolTombstone school, ChallengeReportsStudentRecord student) {}

    private static final Set<String> INDEPENDENT_CATEGORIES = Set.of("INDEPEND", "INDP_FNS");
    private static final Set<String> PUBLIC_CATEGORIES = Set.of("PUBLIC");

    private static final Comparator<SchoolStudent> PROVINCIAL_REPORT_ORDER =
            Comparator.comparing((SchoolStudent schoolStudent) -> schoolStudent.school().getMincode())
                    .thenComparing(schoolStudent -> schoolStudent.student().getPen(), Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(schoolStudent -> schoolStudent.student().getCourseSession(), Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(schoolStudent -> schoolStudent.student().getCourseCodeAndLevel(), Comparator.nullsLast(Comparator.naturalOrder()));

    private final ChallengeReportsSessionRepository challengeReportsSessionRepository;
    private final ChallengeReportsService challengeReportsService;
    private final RestUtils restUtils;

    private SchoolTombstone getSchool(UUID schoolID) {
        return restUtils.getSchoolBySchoolID(schoolID.toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "schoolID", schoolID.toString()));
    }

    public DownloadableReportResponse generateChallengeReportForThisYear(String districtID) throws JsonProcessingException {
        var currentReportingPeriod = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "reportingPeriodSession", null));

        var finalStudentDistrictList = new ArrayList<ChallengeReportsStudentRecord>();
        var currentStage = currentReportingPeriod.getChallengeReportsStatusCode();
        ByteArrayOutputStream byteArrayOutputStream;

        if (currentStage.equalsIgnoreCase(ChallengeReportsStatus.PRELIM.toString())) {
            var fullStudentList = challengeReportsService.getAndGeneratePreliminaryChallengeStudentList(currentReportingPeriod, districtID);
            fullStudentList.forEach(student -> {
                var currentSchool = restUtils.getSchoolBySchoolID(student.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "schoolID", student.getSchoolID().toString()));
                if(student.getDistrictID().toString().equalsIgnoreCase(districtID) && currentSchool.getSchoolCategoryCode().equalsIgnoreCase("PUBLIC")) {
                    finalStudentDistrictList.add(student);
                }
            });
        } else {
            var postedStudents = challengeReportsService.getPostedStudents(currentReportingPeriod, ChallengeReportsStage.FINAL);
            postedStudents.forEach(student -> {
                var currentSchool = restUtils.getSchoolBySchoolID(student.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "schoolID", student.getSchoolID().toString()));
                if(student.getDistrictID().toString().equalsIgnoreCase(districtID) && currentSchool.getSchoolCategoryCode().equalsIgnoreCase("PUBLIC")) {
                    finalStudentDistrictList.add(student);
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

    private List<String> prepareStudentDataForIndependentFundingReport(SchoolTombstone school, ChallengeReportsStudentRecord student, List<IndependentSchoolFundingGroupSnapshot> schoolGroups) {
        var fundingGroup = challengeReportsService.getFundingGroupForCourse(schoolGroups, student.getCourseCodeAndLevel());

        return new ArrayList<>(Arrays.asList(
                school.getMincode(),
                school.getDisplayName(),
                StringUtils.isBlank(fundingGroup) ? "" : fundingGroup,
                student.getPen(),
                student.getCourseCodeAndLevel()
        ));
    }

    private List<String> prepareStudentDataForProvincialReport(SchoolTombstone school, ChallengeReportsStudentRecord student) {
        var courseCodeAndLevel = StringUtils.defaultString(student.getCourseCodeAndLevel());
        var courseCode = courseCodeAndLevel.length() > 5 ? courseCodeAndLevel.substring(0, 5) : courseCodeAndLevel;
        var courseLevel = courseCodeAndLevel.length() > 5 ? courseCodeAndLevel.substring(5) : "";

        return new ArrayList<>(Arrays.asList(
                school.getMincode(),
                school.getDisplayName(),
                StringUtils.defaultString(student.getFundingGroup()),
                student.getPen(),
                StringUtils.defaultString(student.getReportDate()),
                StringUtils.defaultString(student.getFundingCode()),
                courseCode.trim(),
                courseLevel.trim(),
                student.getCourseSession()
        ));
    }

    public DownloadableReportResponse generateDistrictFundingReport() {
        var currentReportingPeriod = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "reportingPeriodSession", null));
        var allStudents = challengeReportsService.getPostedStudents(currentReportingPeriod, ChallengeReportsStage.FINAL);
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
        var allStudents = challengeReportsService.getPostedStudents(currentReportingPeriod, ChallengeReportsStage.FINAL);
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


            var finalStudents = new ArrayList<ChallengeReportsStudentRecord>();
            allStudents.forEach(challengeReportsStudentRecord -> {
                var school = restUtils.getSchoolBySchoolID(challengeReportsStudentRecord.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "schoolID", challengeReportsStudentRecord.getSchoolID().toString()));
                if(school.getSchoolCategoryCode().equalsIgnoreCase("INDEPEND") || school.getSchoolCategoryCode().equalsIgnoreCase("INDP_FNS")) {
                    finalStudents.add(challengeReportsStudentRecord);
                }
            });

            var schoolGroups = restUtils.getSchoolFundingGroupsForCollection(collection.getContent().get(0).getCollectionID());
            log.debug("School funding groups: {}", schoolGroups);
            for (var student : finalStudents) {
                var school = restUtils.getSchoolBySchoolID(student.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, "schoolID", student.getSchoolID().toString()));
                var filteredSchoolGroups = schoolGroups.stream().filter(schoolGroup -> schoolGroup.getSchoolID().equals(student.getSchoolID().toString())).toList();
                List<String> csvRowData = prepareStudentDataForIndependentFundingReport(school, student, filteredSchoolGroups);
                if(csvRowData != null) {
                    csvPrinter.printRecord(csvRowData);
                }
            }

            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();

            downloadableReport.setReportType(MinistryReportTypeCode.INDEPENDENT_SCHOOL_FUNDING_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new ChallengeReportsAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateProvincialReport(MinistryReportTypeCode reportType) throws JsonProcessingException {
        var currentReportingPeriod = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "reportingPeriodSession", null));
        var currentStatus = currentReportingPeriod.getChallengeReportsStatusCode();

        var isPrelimReport = reportType == MinistryReportTypeCode.PRELIM_PROVINCIAL_DISTRICT_REPORT || reportType == MinistryReportTypeCode.PRELIM_PROVINCIAL_INDEPENDENT_REPORT;
        var isIndependentReport = reportType == MinistryReportTypeCode.PRELIM_PROVINCIAL_INDEPENDENT_REPORT || reportType == MinistryReportTypeCode.FINAL_PROVINCIAL_INDEPENDENT_REPORT;

        List<ChallengeReportsStudentRecord> students;
        if (isPrelimReport && currentStatus.equalsIgnoreCase(ChallengeReportsStatus.PRELIM.toString())) {
            students = challengeReportsService.getAndGeneratePreliminaryChallengeStudentList(currentReportingPeriod, null);
        } else {
            students = challengeReportsService.getPostedStudents(currentReportingPeriod, isPrelimReport ? ChallengeReportsStage.PRELIM : ChallengeReportsStage.FINAL);
        }

        String[] headers = ProvincialReportHeader.getAllValuesAsStringArray();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            var categories = isIndependentReport ? INDEPENDENT_CATEGORIES : PUBLIC_CATEGORIES;

            var rows = students.stream()
                    .map(student -> new SchoolStudent(getSchool(student.getSchoolID()), student))
                    .filter(schoolStudent -> categories.contains(schoolStudent.school().getSchoolCategoryCode()))
                    .sorted(PROVINCIAL_REPORT_ORDER)
                    .map(schoolStudent -> prepareStudentDataForProvincialReport(schoolStudent.school(), schoolStudent.student()))
                    .toList();

            csvPrinter.printRecords(rows);
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(reportType.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new ChallengeReportsAPIRuntimeException(e);
        }
    }

}
