package ca.bc.gov.educ.challengeReports.api.repository.v1;

import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChallengeReportsSessionRepository extends JpaRepository<ChallengeReportsSessionEntity, UUID> {

    @Query("SELECT rp FROM ChallengeReportsSessionEntity rp WHERE CURRENT_TIMESTAMP BETWEEN rp.challengeReportsPeriod.activeFromDate AND rp.challengeReportsPeriod.activeToDate")
    Optional<ChallengeReportsSessionEntity> findActiveReportingPeriodSession();
}
