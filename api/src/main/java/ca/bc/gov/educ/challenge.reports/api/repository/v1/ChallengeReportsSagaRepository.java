package ca.bc.gov.educ.challenge.reports.api.repository.v1;


import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The interface Saga repository.
 */
@Repository
public interface ChallengeReportsSagaRepository extends JpaRepository<ChallengeReportsSagaEntity, UUID>, JpaSpecificationExecutor<ChallengeReportsSagaEntity> {
    @Transactional
    @Modifying
    @Query("delete from ChallengeReportsSagaEntity where createDate <= :createDate")
    void deleteByCreateDateBefore(LocalDateTime createDate);

    List<ChallengeReportsSagaEntity> findTop500ByStatusInOrderByCreateDate(List<String> statuses);
}
