package ca.bc.gov.educ.challenge.reports.api.repository.v1;


import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * The interface Saga repository.
 */
@Repository
public interface ChallengeReportsSagaRepository extends JpaRepository<ChallengeReportsSagaEntity, UUID>, JpaSpecificationExecutor<ChallengeReportsSagaEntity> {

}
