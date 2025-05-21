package ca.bc.gov.educ.challenge.reports.api.endpoint.v1;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.URL;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsSession;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.HasChallengeReportsStudentsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RequestMapping(URL.BASE_URL)
public interface ChallengeReportsAPIEndpoint {

  @GetMapping("/district/{districtID}/download")
  @PreAuthorize("hasAuthority('SCOPE_READ_CHALLENGE_REPORTS')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  DownloadableReportResponse getDownloadableChallengeReport(@PathVariable UUID districtID) throws JsonProcessingException;


  @GetMapping("/district/{districtID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_CHALLENGE_REPORTS')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  HasChallengeReportsStudentsResponse getHasChallengeReportStudents(@PathVariable UUID districtID) throws JsonProcessingException;

  @GetMapping("/activeSession")
  @PreAuthorize("hasAuthority('SCOPE_READ_CHALLENGE_REPORTS')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  ChallengeReportsSession getChallengeReportsSession();

  @PutMapping("/{challengeReportSessionID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_CHALLENGE_REPORTS')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Challenge Reports Session", description = "Endpoints to update challenge reports session.")
  @Schema(name = "ChallengeReportsSession", implementation = ChallengeReportsSession.class)
  ChallengeReportsSession updateChallengeReportsSessionAttributes(@Validated @RequestBody ChallengeReportsSession challengeReportsSession, @PathVariable UUID challengeReportSessionID) throws JsonProcessingException;

  @PostMapping("/email/{currentStage}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_CHALLENGE_REPORTS')")
  @Transactional(readOnly = true)
  @ResponseStatus(CREATED)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  ResponseEntity<String> generateAndSendSampleEmail(@PathVariable String currentStage);

  @PostMapping("/preliminaryStage/{updateUser}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_CHALLENGE_REPORTS')")
  @Transactional(readOnly = true)
  @ResponseStatus(OK)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  ResponseEntity<String> startPreliminaryStage(@PathVariable String updateUser);

  @PostMapping("/finalStage/{updateUser}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_CHALLENGE_REPORTS')")
  @Transactional(readOnly = true)
  @ResponseStatus(OK)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  ResponseEntity<String> startFinalStage(@PathVariable String updateUser);
}
