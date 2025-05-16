package ca.bc.gov.educ.challenge.reports.api.repository.v1;


import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Saga event repository.
 */
@Repository
public interface ChallengeReportsSagaEventRepository extends JpaRepository<ChallengeReportsSagaEventEntity, UUID> {
  /**
   * Find by saga list.
   *
   * @param saga the saga
   * @return the list
   */
  List<ChallengeReportsSagaEventEntity> findBySaga(ChallengeReportsSagaEntity saga);

  /**
   * Find by saga and saga event outcome and saga event state and saga step number optional.
   *
   * @param saga         the saga
   * @param eventOutcome the event outcome
   * @param eventState   the event state
   * @param stepNumber   the step number
   * @return the optional
   */
  Optional<ChallengeReportsSagaEventEntity> findBySagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(ChallengeReportsSagaEntity saga, String eventOutcome, String eventState, int stepNumber);

}
