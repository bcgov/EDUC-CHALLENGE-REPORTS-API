package ca.bc.gov.educ.challenge.reports.api.service.v1;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportTypeCode;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.DistrictReportHeader;
import ca.bc.gov.educ.challenge.reports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challenge.reports.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsPostedStudentRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challenge.reports.api.rest.RestUtils;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsStudentRecord;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.SchoolTombstone;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;


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

}
