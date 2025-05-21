package ca.bc.gov.educ.challenge.reports.api.endpoint.v1;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.URL;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(URL.BASE_URL)
public interface MinistryReportsAPIEndpoint {

  @GetMapping("/report/{reportType}/download")
  @PreAuthorize("hasAuthority('SCOPE_READ_CHALLENGE_REPORTS')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  DownloadableReportResponse getMinistryChallengeReport(@PathVariable String reportType);

}
