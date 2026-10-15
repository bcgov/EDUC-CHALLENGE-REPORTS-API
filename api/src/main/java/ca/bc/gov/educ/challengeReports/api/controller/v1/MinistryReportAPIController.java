package ca.bc.gov.educ.challengeReports.api.controller.v1;

import ca.bc.gov.educ.challengeReports.api.constants.v1.MinistryReportTypeCode;
import ca.bc.gov.educ.challengeReports.api.endpoint.v1.MinistryReportsAPIEndpoint;
import ca.bc.gov.educ.challengeReports.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.challengeReports.api.exception.errors.ApiError;
import ca.bc.gov.educ.challengeReports.api.service.v1.CSVReportService;
import ca.bc.gov.educ.challengeReports.api.struct.v1.DownloadableReportResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
public class MinistryReportAPIController implements MinistryReportsAPIEndpoint {

    private final CSVReportService csvReportService;

    public MinistryReportAPIController(CSVReportService csvReportService) {
        this.csvReportService = csvReportService;
    }

    @Override
    public DownloadableReportResponse getMinistryChallengeReport(String reportType) throws JsonProcessingException {
        Optional<MinistryReportTypeCode> code = MinistryReportTypeCode.findByValue(reportType);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch(code.get()) {
            case DISTRICT_FUNDING_REPORT -> csvReportService.generateDistrictFundingReport();
            case INDEPENDENT_SCHOOL_FUNDING_REPORT -> csvReportService.generateIndependentSchoolsFundingReport();
        };
    }
}
