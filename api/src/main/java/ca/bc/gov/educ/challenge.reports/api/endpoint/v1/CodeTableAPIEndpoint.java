package ca.bc.gov.educ.challenge.reports.api.endpoint.v1;

import ca.bc.gov.educ.challenge.reports.api.constants.v1.URL;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportStatusCode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping(URL.BASE_URL)
public interface CodeTableAPIEndpoint {

    @PreAuthorize("hasAuthority('SCOPE_READ_CHALLENGE_REPORTS_CODES')")
    @GetMapping(URL.CHALLENGE_REPORT_STATUS_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Challenge Report Status Codes", description = "Endpoints to get challenge report status codes.")
    @Schema(name = "ChallengeReportStatusCode", implementation = ChallengeReportStatusCode.class)
    List<ChallengeReportStatusCode> getChallengeReportStatusCodes();

}
