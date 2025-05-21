package ca.bc.gov.educ.challenge.reports.api.orchestrator.base;

import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * The interface Orchestrator.
 */
public interface Orchestrator {


  /**
   * Gets saga name.
   *
   * @return the saga name
   */
  String getSagaName();

  /**
   * Start saga.
   *
   * @param saga  the saga data
   */
  void startSaga(ChallengeReportsSagaEntity saga);

  ChallengeReportsSagaEntity createSaga(final String payload, final UUID challengeReportsSessionID, final String userName);

  ChallengeReportsSagaEntity createSaga(ChallengeReportsSagaEntity saga);

  /**
   * Replay saga.
   *
   * @param saga the saga
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  void replaySaga(ChallengeReportsSagaEntity saga) throws IOException, InterruptedException, TimeoutException;
}
