package ca.bc.gov.educ.challenge.reports.api.service.v1;

import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportStatusCodeEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportStatusCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CodeTableService {
  private final ChallengeReportStatusCodeRepository challengeReportStatusCodeRepository;

  @Cacheable("challengeReportStatusCodes")
  public List<ChallengeReportStatusCodeEntity> getAllChallengeReportStatusCodes() {
    return challengeReportStatusCodeRepository.findAll();
  }
}
