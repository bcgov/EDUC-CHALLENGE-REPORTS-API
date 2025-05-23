package ca.bc.gov.educ.challenge.reports.api.service.v1;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.SagaStatusEnum;
import ca.bc.gov.educ.challenge.reports.api.helpers.LogHelper;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;
import ca.bc.gov.educ.challenge.reports.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSagaRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class EventTaskSchedulerAsyncService {
  private final ChallengeReportsSagaRepository sagaRepository;
  private final Map<String, Orchestrator> sagaOrchestrators = new HashMap<>();
  @Setter
  private List<String> statusFilters;

  public EventTaskSchedulerAsyncService(final List<Orchestrator> orchestrators, final ChallengeReportsSagaRepository sagaRepository) {
      this.sagaRepository = sagaRepository;
      orchestrators.forEach(orchestrator -> this.sagaOrchestrators.put(orchestrator.getSagaName(), orchestrator));
  }

  @Async("processUncompletedSagasTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findAndProcessUncompletedSagas() {
    log.debug("Processing uncompleted sagas");
    final var sagas = this.sagaRepository.findTop500ByStatusInOrderByCreateDate(this.getStatusFilters());
    log.debug("Found {} sagas to be retried", sagas.size());
    if (!sagas.isEmpty()) {
      this.processUncompletedSagas(sagas);
    }
  }

  private void processUncompletedSagas(final List<ChallengeReportsSagaEntity> sagas) {
    for (val saga : sagas) {
      if (saga.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(2))
        && this.sagaOrchestrators.containsKey(saga.getSagaName())) {
        try {
          this.setRetryCountAndLog(saga);
          this.sagaOrchestrators.get(saga.getSagaName()).replaySaga(saga);
        } catch (final InterruptedException ex) {
          Thread.currentThread().interrupt();
          log.error("InterruptedException while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, ex);
        } catch (final IOException | TimeoutException e) {
          log.error("Exception while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, e);
        }
      }
    }
  }

  public List<String> getStatusFilters() {
    if (this.statusFilters != null && !this.statusFilters.isEmpty()) {
      return this.statusFilters;
    } else {
      final var statuses = new ArrayList<String>();
      statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
      statuses.add(SagaStatusEnum.STARTED.toString());
      return statuses;
    }
  }

  private void setRetryCountAndLog(final ChallengeReportsSagaEntity saga) {
    Integer retryCount = saga.getRetryCount();
    if (retryCount == null || retryCount == 0) {
      retryCount = 1;
    } else {
      retryCount += 1;
    }
    saga.setRetryCount(retryCount);
    this.sagaRepository.save(saga);
    LogHelper.logSagaRetry(saga);
  }
}
