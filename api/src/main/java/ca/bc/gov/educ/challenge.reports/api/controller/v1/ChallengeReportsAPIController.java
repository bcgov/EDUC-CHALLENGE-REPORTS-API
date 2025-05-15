package ca.bc.gov.educ.challenge.reports.api.controller.v1;

import ca.bc.gov.educ.challenge.reports.api.endpoint.v1.ChallengeReportsAPIEndpoint;
import ca.bc.gov.educ.challenge.reports.api.mappers.v1.ChallengeReportSessionMapper;
import ca.bc.gov.educ.challenge.reports.api.service.v1.CSVReportService;
import ca.bc.gov.educ.challenge.reports.api.service.v1.ChallengeReportsService;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsSession;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.challenge.reports.api.util.RequestUtil;
import ca.bc.gov.educ.challenge.reports.api.util.ValidationUtil;
import ca.bc.gov.educ.challenge.reports.api.validator.ChallengeReportsSessionValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Slf4j
public class ChallengeReportsAPIController implements ChallengeReportsAPIEndpoint {

    private final CSVReportService csvReportService;
    private final ChallengeReportsService challengeReportsService;
    private final ChallengeReportsSessionValidator challengeReportsSessionValidator;
    private final ChallengeReportSessionMapper challengeReportSessionMapper = ChallengeReportSessionMapper.mapper;

    public ChallengeReportsAPIController(CSVReportService csvReportService, ChallengeReportsService challengeReportsService, ChallengeReportsSessionValidator challengeReportsSessionValidator) {
        this.csvReportService = csvReportService;
        this.challengeReportsService = challengeReportsService;
        this.challengeReportsSessionValidator = challengeReportsSessionValidator;
    }

    @Override
    public DownloadableReportResponse getDownloadableChallengeReport(UUID districtID) throws JsonProcessingException {
        return csvReportService.generateChallengeReportForThisYear(districtID.toString());
    }

    @Override
    public ChallengeReportsSession getChallengeReportsSession() {
        return challengeReportSessionMapper.toStructure(challengeReportsService.getChallengeReportActiveSession());
    }

    @Override
    public ChallengeReportsSession updateChallengeReportsSessionAttributes(ChallengeReportsSession challengeReportsSession, UUID challengeReportSessionID) {
        ValidationUtil.validatePayload(() -> this.challengeReportsSessionValidator.validatePayload(challengeReportsSession));
        RequestUtil.setAuditColumnsForUpdate(challengeReportsSession);
        return challengeReportSessionMapper.toStructure(challengeReportsService.updateChallengeReportsSessionAttributes(challengeReportSessionMapper.toModel(challengeReportsSession)));
    }

    @Override
    public ResponseEntity<String> generateAndSendSampleEmail(String currentStage) {
        return null;
    }

    @Override
    public ResponseEntity<String> startPreliminaryStage() {
        return null;
    }

    @Override
    public ResponseEntity<String> startFinalStage() {
        return null;
    }

}
