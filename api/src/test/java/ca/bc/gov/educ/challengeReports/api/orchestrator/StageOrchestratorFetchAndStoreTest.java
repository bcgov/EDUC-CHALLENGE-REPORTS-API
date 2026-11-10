package ca.bc.gov.educ.challengeReports.api.orchestrator;

import ca.bc.gov.educ.challengeReports.api.constants.v1.ChallengeReportsStage;
import ca.bc.gov.educ.challengeReports.api.messaging.MessagePublisher;
import ca.bc.gov.educ.challengeReports.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsSagaEntity;
import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challengeReports.api.service.v1.ChallengeReportsService;
import ca.bc.gov.educ.challengeReports.api.service.v1.EmailService;
import ca.bc.gov.educ.challengeReports.api.service.v1.SagaService;
import ca.bc.gov.educ.challengeReports.api.struct.v1.ChallengeReportsStudentRecord;
import ca.bc.gov.educ.challengeReports.api.struct.v1.Event;
import ca.bc.gov.educ.challengeReports.api.struct.v1.FinalStageSagaData;
import ca.bc.gov.educ.challengeReports.api.struct.v1.PreliminaryStageSagaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.challengeReports.api.constants.v1.EventOutcome.SESSION_STATUS_UPDATED;
import static ca.bc.gov.educ.challengeReports.api.constants.v1.EventOutcome.STUDENTS_FETCHED_AND_STORED;
import static ca.bc.gov.educ.challengeReports.api.constants.v1.EventType.FETCH_AND_STORE_STUDENTS;
import static ca.bc.gov.educ.challengeReports.api.constants.v1.EventType.UPDATE_SESSION_STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StageOrchestratorFetchAndStoreTest {

  private SagaService sagaService;
  private MessagePublisher messagePublisher;
  private Publisher publisher;
  private ChallengeReportsService challengeReportsService;
  private ChallengeReportsSessionEntity session;
  private ChallengeReportsSagaEntity saga;
  private Event event;

  @BeforeEach
  void setUp() {
    sagaService = mock(SagaService.class);
    messagePublisher = mock(MessagePublisher.class);
    publisher = mock(Publisher.class);
    challengeReportsService = mock(ChallengeReportsService.class);

    session = new ChallengeReportsSessionEntity();
    session.setChallengeReportsSessionID(UUID.randomUUID());
    when(challengeReportsService.getChallengeReportActiveSession()).thenReturn(session);

    saga = ChallengeReportsSagaEntity.builder().sagaId(UUID.randomUUID()).build();
    event = Event.builder().sagaId(saga.getSagaId()).eventType(UPDATE_SESSION_STATUS).eventOutcome(SESSION_STATUS_UPDATED).eventPayload("{}").build();
  }

  @Test
  void preliminary_whenNoRowsSaved_buildsAndSavesPrelimRows() throws Exception {
    var students = List.of(new ChallengeReportsStudentRecord());
    when(challengeReportsService.hasPostedStudents(session, ChallengeReportsStage.PRELIM)).thenReturn(false);
    when(challengeReportsService.getAndGeneratePreliminaryChallengeStudentList(session)).thenReturn(students);

    preliminaryOrchestrator().fetchAndStorePreliminarySetOfStudents(event, saga, PreliminaryStageSagaData.builder().updateUser("ABC").build());

    verify(challengeReportsService).savePostedStudentsIfAbsent(students, session, "ABC", ChallengeReportsStage.PRELIM);
    assertNextEventPosted();
  }

  @Test
  void preliminary_whenRowsAlreadySaved_skipsFetchAndSave() throws Exception {
    when(challengeReportsService.hasPostedStudents(session, ChallengeReportsStage.PRELIM)).thenReturn(true);

    preliminaryOrchestrator().fetchAndStorePreliminarySetOfStudents(event, saga, PreliminaryStageSagaData.builder().updateUser("ABC").build());

    verify(challengeReportsService, never()).getAndGeneratePreliminaryChallengeStudentList(any());
    verify(challengeReportsService, never()).savePostedStudentsIfAbsent(any(), any(), any(), any());
    assertNextEventPosted();
  }

  @Test
  void final_whenNoRowsSaved_buildsAndSavesFinalRows() throws Exception {
    var students = List.of(new ChallengeReportsStudentRecord());
    when(challengeReportsService.hasPostedStudents(session, ChallengeReportsStage.FINAL)).thenReturn(false);
    when(challengeReportsService.getAndGeneratePreliminaryChallengeStudentList(session)).thenReturn(students);

    finalOrchestrator().fetchAndStoreFinalSetOfStudents(event, saga, FinalStageSagaData.builder().updateUser("ABC").build());

    verify(challengeReportsService).savePostedStudentsIfAbsent(students, session, "ABC", ChallengeReportsStage.FINAL);
    assertNextEventPosted();
  }

  @Test
  void final_whenRowsAlreadySaved_skipsFetchAndSave() throws Exception {
    when(challengeReportsService.hasPostedStudents(session, ChallengeReportsStage.FINAL)).thenReturn(true);

    finalOrchestrator().fetchAndStoreFinalSetOfStudents(event, saga, FinalStageSagaData.builder().updateUser("ABC").build());

    verify(challengeReportsService, never()).getAndGeneratePreliminaryChallengeStudentList(any());
    verify(challengeReportsService, never()).savePostedStudentsIfAbsent(any(), any(), any(), any());
    assertNextEventPosted();
  }

  private void assertNextEventPosted() {
    assertThat(saga.getSagaState()).isEqualTo(FETCH_AND_STORE_STUDENTS.toString());
    var eventCaptor = ArgumentCaptor.forClass(Event.class);
    verify(publisher).dispatchChoreographyEvent(eventCaptor.capture(), eq(saga));
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(FETCH_AND_STORE_STUDENTS);
    assertThat(eventCaptor.getValue().getEventOutcome()).isEqualTo(STUDENTS_FETCHED_AND_STORED);
    verify(messagePublisher).dispatchMessage(anyString(), any(byte[].class));
  }

  private PreliminaryStageOrchestrator preliminaryOrchestrator() {
    return new PreliminaryStageOrchestrator(sagaService, messagePublisher, challengeReportsService, mock(EmailService.class), publisher);
  }

  private FinalStageOrchestrator finalOrchestrator() {
    return new FinalStageOrchestrator(sagaService, messagePublisher, challengeReportsService, mock(EmailService.class), publisher);
  }
}
