package ca.bc.gov.educ.challenge.reports.api.service.v1;

import ca.bc.gov.educ.challenge.reports.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challenge.reports.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ChallengeReportsService {
  private final ChallengeReportsSessionRepository challengeReportsSessionRepository;

  public ChallengeReportsSessionEntity updateChallengeReportsSessionAttributes(ChallengeReportsSessionEntity challengeReportsSessionEntity) {
    var currentSession = this.challengeReportsSessionRepository.findById(challengeReportsSessionEntity.getChallengeReportsSessionID()).orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "challengeReportsSession", challengeReportsSessionEntity.getChallengeReportsSessionID().toString()));
    BeanUtils.copyProperties(challengeReportsSessionEntity, currentSession, "challengeReportsSessionID", "challengeReportsStatusCode");
    TransformUtil.uppercaseFields(currentSession);
    return this.challengeReportsSessionRepository.save(currentSession);
  }
}
