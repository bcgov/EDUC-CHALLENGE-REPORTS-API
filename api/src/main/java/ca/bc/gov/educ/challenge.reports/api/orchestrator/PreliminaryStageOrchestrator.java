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
import ca.bc.gov.educ.challenge.reports.api.struct.v1.PreliminaryStageSagaData;
import ca.bc.gov.educ.challenge.reports.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.challenge.reports.api.constants.v1.EventOutcome.PRELIMINARY_EMAIL_SENT;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.EventOutcome.SESSION_STATUS_UPDATED;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.EventType.SEND_OUT_PRELIMINARY_EMAIL;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.EventType.UPDATE_SESSION_STATUS;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.SagaStatusEnum.IN_PROGRESS;

@Component
@Slf4j
public class PreliminaryStageOrchestrator extends BaseOrchestrator<PreliminaryStageSagaData> {

    private final ChallengeReportsService challengeReportsService;
    private final EmailService emailService;
    private final Publisher publisher;

    protected PreliminaryStageOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, ChallengeReportsService challengeReportsService, EmailService emailService, Publisher publisher) {
        super(sagaService, messagePublisher, PreliminaryStageSagaData.class, SagaEnum.PRELIMINARY_STAGE_SAGA.toString(), TopicsEnum.PRELIMINARY_STAGE_TOPIC.toString());
        this.challengeReportsService = challengeReportsService;
        this.emailService = emailService;
        this.publisher = publisher;
    }
    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(UPDATE_SESSION_STATUS, this::updateSessionStatus)
                .step(UPDATE_SESSION_STATUS, SESSION_STATUS_UPDATED, SEND_OUT_PRELIMINARY_EMAIL, this::sendPreliminaryStageEmails)
                .end(SEND_OUT_PRELIMINARY_EMAIL, PRELIMINARY_EMAIL_SENT);
    }

    public void sendPreliminaryStageEmails(final Event event, final ChallengeReportsSagaEntity saga, final PreliminaryStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SEND_OUT_PRELIMINARY_EMAIL.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        emailService.sendPreliminaryEmailTo1701Admin();

        postEvent(saga, sagaData, SEND_OUT_PRELIMINARY_EMAIL, PRELIMINARY_EMAIL_SENT);
    }

    public void updateSessionStatus(final Event event, final ChallengeReportsSagaEntity saga, final PreliminaryStageSagaData sagaData) throws JsonProcessingException {
        final ChallengeReportsSagaEventEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_SESSION_STATUS.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        challengeReportsService.updateChallengeReportsStatus(ChallengeReportsStatus.PRELIM, sagaData.getUpdateUser());
        emailService.sendPreliminaryEmailTo1701Admin();
        postEvent(saga, sagaData, UPDATE_SESSION_STATUS, SESSION_STATUS_UPDATED);
    }

    private void postEvent(final ChallengeReportsSagaEntity saga, final PreliminaryStageSagaData collectionSagaData, EventType eventType, EventOutcome eventOutcome) throws JsonProcessingException{
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
