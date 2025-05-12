package ca.bc.gov.educ.challenge.reports.api.repository.v1;

import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportStatusCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChallengeReportStatusCodeRepository extends JpaRepository<ChallengeReportStatusCodeEntity, String> {
}
