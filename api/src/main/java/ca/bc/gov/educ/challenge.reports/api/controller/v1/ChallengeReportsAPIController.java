package ca.bc.gov.educ.challenge.reports.api.controller.v1;

import ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.SagaEnum;
import ca.bc.gov.educ.challenge.reports.api.endpoint.v1.ChallengeReportsAPIEndpoint;
import ca.bc.gov.educ.challenge.reports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challenge.reports.api.mappers.v1.ChallengeReportSessionMapper;
import ca.bc.gov.educ.challenge.reports.api.mappers.v1.SagaDataMapper;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;
import ca.bc.gov.educ.challenge.reports.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.challenge.reports.api.service.v1.CSVReportService;
import ca.bc.gov.educ.challenge.reports.api.service.v1.ChallengeReportsService;
import ca.bc.gov.educ.challenge.reports.api.service.v1.EmailService;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.*;
import ca.bc.gov.educ.challenge.reports.api.util.RequestUtil;
import ca.bc.gov.educ.challenge.reports.api.util.ValidationUtil;
import ca.bc.gov.educ.challenge.reports.api.validator.ChallengeReportsSessionValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static ca.bc.gov.educ.challenge.reports.api.constants.v1.SagaEnum.FINAL_STAGE_SAGA;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.SagaEnum.PRELIMINARY_STAGE_SAGA;

@RestController
@Slf4j
public class ChallengeReportsAPIController implements ChallengeReportsAPIEndpoint {

    private final CSVReportService csvReportService;
    private final ChallengeReportsService challengeReportsService;
    private final EmailService emailService;
    private final ChallengeReportsSessionValidator challengeReportsSessionValidator;
    private final ChallengeReportSessionMapper challengeReportSessionMapper = ChallengeReportSessionMapper.mapper;
    private final Map<String, Orchestrator> orchestratorMap = new HashMap<>();
    private static final SagaDataMapper sagaDataMapper = SagaDataMapper.mapper;

    public ChallengeReportsAPIController(CSVReportService csvReportService, ChallengeReportsService challengeReportsService, ChallengeReportsSessionValidator challengeReportsSessionValidator, List<Orchestrator> orchestrators, EmailService emailService) {
        this.csvReportService = csvReportService;
        this.challengeReportsService = challengeReportsService;
        this.challengeReportsSessionValidator = challengeReportsSessionValidator;
        this.emailService = emailService;
        orchestrators.forEach(orchestrator -> this.orchestratorMap.put(orchestrator.getSagaName(), orchestrator));
        log.info("'{}' Saga Orchestrators are loaded.", String.join(",", this.orchestratorMap.keySet()));
    }

    @Override
    public DownloadableReportResponse getDownloadableChallengeReport(UUID districtID) throws JsonProcessingException {
        return csvReportService.generateChallengeReportForThisYear(districtID.toString());
    }

    @Override
    public HasChallengeReportsStudentsResponse getHasChallengeReportStudents(UUID districtID) throws JsonProcessingException {
        return challengeReportsService.getHasChallengeReportStudents(districtID.toString());
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
        final List<FieldError> apiValidationErrors = new ArrayList<>();

        if (!EnumUtils.isValidEnum(ChallengeReportsStatus.class, currentStage)) {
            apiValidationErrors.add(ValidationUtil.createFieldError("currentStage", "currentStage", currentStage, "Invalid challenge reports status provided."));
        }
        ValidationUtil.validatePayload(apiValidationErrors);
        emailService.sendSampleEmailToStaff(currentStage);
        return ResponseEntity.status(HttpStatus.CREATED).body("Email sent successfully.");
    }

    @Override
    public ResponseEntity<String> startPreliminaryStage(String updateUser) {
        PreliminaryStageSagaData sagaData = new PreliminaryStageSagaData();
        sagaData.setUpdateUser(updateUser);
        sagaData.setChallengeReportSessionID(challengeReportsService.getChallengeReportActiveSession().getChallengeReportsSessionID().toString());
        return processPreliminaryStageSaga(sagaData);
    }

    @Override
    public ResponseEntity<String> startFinalStage(String updateUser) {
        FinalStageSagaData sagaData = new FinalStageSagaData();
        sagaData.setUpdateUser(updateUser);
        sagaData.setChallengeReportSessionID(challengeReportsService.getChallengeReportActiveSession().getChallengeReportsSessionID().toString());
        return processFinalStageSaga(sagaData);
    }

    private ResponseEntity<String> processPreliminaryStageSaga(PreliminaryStageSagaData sagaData) {
        try {
            ChallengeReportsSagaEntity sagaEntity = sagaDataMapper.toModel(String.valueOf(PRELIMINARY_STAGE_SAGA), sagaData);
            return processServicesSaga(PRELIMINARY_STAGE_SAGA, sagaEntity);
        } catch (JsonProcessingException e) {
            throw new ChallengeReportsAPIRuntimeException(e);
        }
    }

    private ResponseEntity<String> processFinalStageSaga(FinalStageSagaData sagaData) {
        try {
            ChallengeReportsSagaEntity sagaEntity = sagaDataMapper.toModel(String.valueOf(FINAL_STAGE_SAGA), sagaData);
            return processServicesSaga(FINAL_STAGE_SAGA, sagaEntity);
        } catch (JsonProcessingException e) {
            throw new ChallengeReportsAPIRuntimeException(e);
        }
    }

    private ResponseEntity<String> processServicesSaga(final SagaEnum sagaName, ChallengeReportsSagaEntity sagaEntity) {
        try {
            final var orchestrator = orchestratorMap.get(sagaName.toString());
            final var saga = this.orchestratorMap
                    .get(sagaName.toString())
                    .createSaga(sagaEntity);
            orchestrator.startSaga(saga);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(saga.getSagaId().toString());
        } catch (final Exception e) {
            throw new ChallengeReportsAPIRuntimeException(e.getMessage());
        }
    }

}
