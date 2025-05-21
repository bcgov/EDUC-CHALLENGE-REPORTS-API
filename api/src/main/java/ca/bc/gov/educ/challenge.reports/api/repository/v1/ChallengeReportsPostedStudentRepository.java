package ca.bc.gov.educ.challenge.reports.api.repository.v1;

import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsPostedStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeReportsPostedStudentRepository extends JpaRepository<ChallengeReportsPostedStudentEntity, UUID> {

    List<ChallengeReportsPostedStudentEntity> findAllByChallengeReportsSessionEntity_ChallengeReportsSessionID(UUID challengeReportsSessionId);
}
