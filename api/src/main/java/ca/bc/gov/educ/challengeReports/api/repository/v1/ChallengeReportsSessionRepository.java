package ca.bc.gov.educ.challengeReports.api.repository.v1;

import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsSessionEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChallengeReportsSessionRepository extends JpaRepository<ChallengeReportsSessionEntity, UUID> {

    @Query("SELECT rp FROM ChallengeReportsSessionEntity rp WHERE CURRENT_TIMESTAMP BETWEEN rp.challengeReportsPeriod.activeFromDate AND rp.challengeReportsPeriod.activeToDate")
    Optional<ChallengeReportsSessionEntity> findActiveReportingPeriodSession();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "60000"))
    @Query("SELECT s FROM ChallengeReportsSessionEntity s WHERE s.challengeReportsSessionID = :challengeReportsSessionID")
    Optional<ChallengeReportsSessionEntity> lockById(@Param("challengeReportsSessionID") UUID challengeReportsSessionID);
}
