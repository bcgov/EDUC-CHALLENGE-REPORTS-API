package ca.bc.gov.educ.challenge.reports.api.orchestrator;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.*;
import ca.bc.gov.educ.challenge.reports.api.messaging.MessagePublisher;
import ca.bc.gov.educ.challenge.reports.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEventEntity;
import ca.bc.gov.educ.challenge.reports.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.challenge.reports.api.service.v1.ChallengeReportsService;
import ca.bc.gov.educ.challenge.reports.api.service.v1.EmailService;
import ca.bc.gov.educ.challenge.reports.api.service.v1.SagaService;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.Event;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.FinalStageSagaData;
import ca.bc.gov.educ.challenge.reports.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.challenge.reports.api.constants.v1.EventOutcome.*;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.EventType.*;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.SagaStatusEnum.IN_PROGRESS;

@Component
@Slf4j
public class FinalStageOrchestrator extends BaseOrchestrator<FinalStageSagaData> {

    private final ChallengeReportsService challengeReportsService;
    private final EmailService emailService;
    private final Publisher publisher;

    protected FinalStageOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, ChallengeReportsService challengeReportsService, EmailService emailService, Publisher publisher) {
        super(sagaService, messagePublisher, FinalStageSagaData.class, SagaEnum.FINAL_STAGE_SAGA.toString(), TopicsEnum.FINAL_STAGE_TOPIC.toString());
        this.challengeReportsService = challengeReportsService;
        this.emailService = emailService;
        this.publisher = publisher;
    }
    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(UPDATE_SESSION_STATUS, this::updateSessionStatus)
                .step(UPDATE_SESSION_STATUS, SESSION_STATUS_UPDATED, FETCH_AND_STORE_STUDENTS, this::fetchAndStoreFinalSetOfStudents)
                .step(FETCH_AND_STORE_STUDENTS, STUDENTS_FETCHED_AND_STORED, SEND_OUT_FINAL_EMAIL, this::sendFinalStageEmails)
                .step(SEND_OUT_FINAL_EMAIL, FINAL_EMAIL_SENT, SEND_OUT_PUBLIC_TEAM_EMAIL, this::sendFinalStagePublicSchoolsEmails)
                .step(SEND_OUT_PUBLIC_TEAM_EMAIL, PUBLIC_TEAM_EMAIL_SENT, SEND_OUT_INDY_TEAM_EMAIL, this::sendFinalStageIndySchoolsEmails)
                .end(SEND_OUT_INDY_TEAM_EMAIL, INDY_TEAM_EMAIL_SENT);
    }

    public void fetchAndStoreFinalSetOfStudents(final Event event, final ChallengeReportsSagaEntity saga, final FinalStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(FETCH_AND_STORE_STUDENTS.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        var currentSession = challengeReportsService.getChallengeReportActiveSession();
        var students = challengeReportsService.getAndGeneratePreliminaryChallengeStudentList(currentSession, null);
        challengeReportsService.saveAllPostedStudents(students, currentSession, sagaData.getUpdateUser());

        postEvent(saga, sagaData, FETCH_AND_STORE_STUDENTS, STUDENTS_FETCHED_AND_STORED);
    }

    public void sendFinalStageEmails(final Event event, final ChallengeReportsSagaEntity saga, final FinalStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SEND_OUT_FINAL_EMAIL.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        emailService.sendFinalEmailTo1701Admin();

        postEvent(saga, sagaData, SEND_OUT_FINAL_EMAIL, FINAL_EMAIL_SENT);
    }

    public void sendFinalStageIndySchoolsEmails(final Event event, final ChallengeReportsSagaEntity saga, final FinalStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SEND_OUT_INDY_TEAM_EMAIL.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        emailService.sendFinalEmailToIndySchoolsTeam();

        postEvent(saga, sagaData, SEND_OUT_INDY_TEAM_EMAIL, INDY_TEAM_EMAIL_SENT);
    }

    public void sendFinalStagePublicSchoolsEmails(final Event event, final ChallengeReportsSagaEntity saga, final FinalStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SEND_OUT_PUBLIC_TEAM_EMAIL.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        emailService.sendFinalEmailToPublicFinanceSchoolsTeam();

        postEvent(saga, sagaData, SEND_OUT_PUBLIC_TEAM_EMAIL, PUBLIC_TEAM_EMAIL_SENT);
    }

    public void updateSessionStatus(final Event event, final ChallengeReportsSagaEntity saga, final FinalStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_SESSION_STATUS.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        challengeReportsService.updateChallengeReportsStatus(ChallengeReportsStatus.FINALIZED, sagaData.getUpdateUser());

        postEvent(saga, sagaData, UPDATE_SESSION_STATUS, SESSION_STATUS_UPDATED);
    }

    private void postEvent(final ChallengeReportsSagaEntity saga, final FinalStageSagaData collectionSagaData, EventType eventType, EventOutcome eventOutcome) throws JsonProcessingException{
        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(eventType)
                .eventOutcome(eventOutcome)
                .eventPayload(JsonUtil.getJsonStringFromObject(collectionSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        publishToJetStream(nextEvent, saga);
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
    }

    private void publishToJetStream(final Event event, ChallengeReportsSagaEntity saga) {
        publisher.dispatchChoreographyEvent(event, saga);
    }
}
