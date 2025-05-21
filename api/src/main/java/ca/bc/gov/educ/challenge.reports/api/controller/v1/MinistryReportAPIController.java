package ca.bc.gov.educ.challenge.reports.api.controller.v1;

import ca.bc.gov.educ.challenge.reports.api.endpoint.v1.MinistryReportsAPIEndpoint;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MinistryReportAPIController implements MinistryReportsAPIEndpoint {

    @Override
    public DownloadableReportResponse getMinistryChallengeReport(String reportType) {
        return null;
    }
}
