package ca.bc.gov.educ.challenge.reports.api.controller.v1;

import ca.bc.gov.educ.challenge.reports.api.endpoint.v1.ChallengeReportsAPIEndpoint;
import ca.bc.gov.educ.challenge.reports.api.service.v1.CSVReportService;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Slf4j
public class ChallengeReportsAPIController implements ChallengeReportsAPIEndpoint {

  private final CSVReportService csvReportService;

    public ChallengeReportsAPIController(CSVReportService csvReportService) {
        this.csvReportService = csvReportService;
    }

    @Override
  public DownloadableReportResponse getDownloadableChallengeReport(UUID districtID) {
    return csvReportService.generateChallengeReportForThisYear(districtID);
  }
}
