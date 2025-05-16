package ca.bc.gov.educ.challenge.reports.api.service.v1;

import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEventEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSagaEventRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSagaRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static ca.bc.gov.educ.challenge.reports.api.constants.v1.EventType.INITIATED;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.SagaStatusEnum.STARTED;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Saga service.
 */
@Service
@Slf4j
public class SagaService {
  /**
   * The Saga repository.
   */
  @Getter(AccessLevel.PRIVATE)
  private final ChallengeReportsSagaRepository challengeReportsSagaRepository;
  /**
   * The Saga event repository.
   */
  @Getter(PRIVATE)
  private final ChallengeReportsSagaEventRepository challengeReportsSagaEventRepository;
  
  @Autowired
  public SagaService(ChallengeReportsSagaRepository challengeReportsSagaRepository, ChallengeReportsSagaEventRepository challengeReportsSagaEventRepository) {
      this.challengeReportsSagaRepository = challengeReportsSagaRepository;
      this.challengeReportsSagaEventRepository = challengeReportsSagaEventRepository;
  }


  /**
   * Create saga record saga.
   *
   * @param saga the saga
   * @return the saga
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public ChallengeReportsSagaEntity createSagaRecord(final ChallengeReportsSagaEntity saga) {
    return challengeReportsSagaRepository.save(saga);
  }

  /**
   * Create saga records.
   *
   * @param sagas the sagas
   * @return the saga
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public List<ChallengeReportsSagaEntity> createSagaRecords(final List<ChallengeReportsSagaEntity> sagas) {
    return challengeReportsSagaRepository.saveAll(sagas);
  }

  /**
   * no need to do a get here as it is an attached entity
   * first find the child record, if exist do not add. this scenario may occur in replay process,
   * so dont remove this check. removing this check will lead to duplicate records in the child table.
   *
   * @param saga            the saga object.
   * @param sagaEventStates the saga event
   */
  @Retryable(maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateAttachedSagaWithEvents(final ChallengeReportsSagaEntity saga, final ChallengeReportsSagaEventEntity sagaEventStates) {
    saga.setUpdateDate(LocalDateTime.now());
    challengeReportsSagaRepository.save(saga);
    val result = challengeReportsSagaEventRepository
      .findBySagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(saga, sagaEventStates.getSagaEventOutcome(), sagaEventStates.getSagaEventState(), sagaEventStates.getSagaStepNumber() - 1); //check if the previous step was same and had same outcome, and it is due to replay.
    if (result.isEmpty()) {
      challengeReportsSagaEventRepository.save(sagaEventStates);
    }
  }

  /**
   * Find saga by id optional.
   *
   * @param sagaId the saga id
   * @return the optional
   */
  public Optional<ChallengeReportsSagaEntity> findSagaById(final UUID sagaId) {
    return challengeReportsSagaRepository.findById(sagaId);
  }

  /**
   * Find all saga states list.
   *
   * @param saga the saga
   * @return the list
   */
  public List<ChallengeReportsSagaEventEntity> findAllSagaStates(final ChallengeReportsSagaEntity saga) {
    return challengeReportsSagaEventRepository.findBySaga(saga);
  }


  /**
   * Update saga record.
   *
   * @param saga the saga
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void updateSagaRecord(final ChallengeReportsSagaEntity saga) { // saga here MUST be an attached entity
    challengeReportsSagaRepository.save(saga);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ChallengeReportsSagaEntity createSagaRecordInDB(final String sagaName, final String userName, final String payload, final UUID challengeReportsSessionId) {
    final var saga = ChallengeReportsSagaEntity
      .builder()
      .challengeReportsSessionId(challengeReportsSessionId)
      .payload(payload)
      .sagaName(sagaName)
      .status(STARTED.toString())
      .sagaState(INITIATED.toString())
      .createDate(LocalDateTime.now())
      .createUser(userName)
      .updateUser(userName)
      .updateDate(LocalDateTime.now())
      .build();
    return this.createSagaRecord(saga);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<ChallengeReportsSagaEntity> createSagaRecordsInDB(final List<ChallengeReportsSagaEntity> sdcSagaEntities) {
    return this.createSagaRecords(sdcSagaEntities);
  }

  /**
   * Find all completable future.
   *
   * @param specs      the saga specs
   * @param pageNumber the page number
   * @param pageSize   the page size
   * @param sorts      the sorts
   * @return the completable future
   */
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public CompletableFuture<Page<ChallengeReportsSagaEntity>> findAll(final Specification<ChallengeReportsSagaEntity> specs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return challengeReportsSagaRepository.findAll(specs, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    });
  }
}
