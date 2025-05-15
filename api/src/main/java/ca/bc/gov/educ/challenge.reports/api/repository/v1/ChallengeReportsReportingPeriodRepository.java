package ca.bc.gov.educ.challenge.reports.api.repository.v1;

import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChallengeReportsReportingPeriodRepository extends JpaRepository<ChallengeReportsReportingPeriodEntity, String> {
    @Query("SELECT rp FROM ChallengeReportsReportingPeriodEntity rp WHERE CURRENT_TIMESTAMP BETWEEN rp.activeFromDate AND rp.activeToDate")
    Optional<ChallengeReportsReportingPeriodEntity> findActiveReportingPeriod();
}
