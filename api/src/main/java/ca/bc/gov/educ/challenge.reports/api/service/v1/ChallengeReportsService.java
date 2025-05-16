package ca.bc.gov.educ.challenge.reports.api.service.v1;

import ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus;
import ca.bc.gov.educ.challenge.reports.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challenge.reports.api.util.RequestUtil;
import ca.bc.gov.educ.challenge.reports.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class ChallengeReportsService {
  private final ChallengeReportsSessionRepository challengeReportsSessionRepository;

  public ChallengeReportsSessionEntity getChallengeReportActiveSession() {
    return challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "challengeReportsSession", "activeSession"));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ChallengeReportsSessionEntity updateChallengeReportsSessionAttributes(ChallengeReportsSessionEntity challengeReportsSessionEntity) {
    var currentSession = this.challengeReportsSessionRepository.findById(challengeReportsSessionEntity.getChallengeReportsSessionID()).orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "challengeReportsSession", challengeReportsSessionEntity.getChallengeReportsSessionID().toString()));
    BeanUtils.copyProperties(challengeReportsSessionEntity, currentSession, "challengeReportsSessionID", "challengeReportsStatusCode");
    TransformUtil.uppercaseFields(currentSession);
    return this.challengeReportsSessionRepository.save(currentSession);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateChallengeReportsStatus(ChallengeReportsStatus status, String updateUser) {
    var currentSession = challengeReportsSessionRepository.findActiveReportingPeriodSession().orElseThrow(() -> new EntityNotFoundException(ChallengeReportsSessionEntity.class, "challengeReportsSession", "activeSession"));
    currentSession.setUpdateDate(LocalDateTime.now());
    currentSession.setUpdateUser(updateUser);
    currentSession.setChallengeReportsStatusCode(status.toString());
    this.challengeReportsSessionRepository.save(currentSession);
  }
}
