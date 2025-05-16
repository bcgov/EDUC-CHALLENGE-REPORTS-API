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
import ca.bc.gov.educ.challenge.reports.api.struct.v1.PreliminaryStageSagaData;
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
                .begin(SEND_OUT_FINAL_EMAIL, this::sendFinalStageEmails)
                .step(SEND_OUT_FINAL_EMAIL, FINAL_EMAIL_SENT, UPDATE_SESSION_STATUS, this::updateSessionStatus)
                .end(UPDATE_SESSION_STATUS, SESSION_STATUS_UPDATED);
    }

    public void sendFinalStageEmails(final Event event, final ChallengeReportsSagaEntity saga, final FinalStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SEND_OUT_FINAL_EMAIL.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        //Change me
        //emailService.sendEmail(null);

        postEvent(saga, sagaData, SEND_OUT_FINAL_EMAIL, FINAL_EMAIL_SENT);
    }

    public void updateSessionStatus(final Event event, final ChallengeReportsSagaEntity saga, final FinalStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_SESSION_STATUS.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        //service call
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
