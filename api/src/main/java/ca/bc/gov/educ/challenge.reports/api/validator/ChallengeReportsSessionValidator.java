package ca.bc.gov.educ.challenge.reports.api.validator;

import ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus;
import ca.bc.gov.educ.challenge.reports.api.service.v1.CodeTableService;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsSession;
import ca.bc.gov.educ.challenge.reports.api.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChallengeReportsSessionValidator {

  @Getter(AccessLevel.PRIVATE)
  private final CodeTableService codeTableService;

  @Autowired
  public ChallengeReportsSessionValidator(CodeTableService codeTableService) {
      this.codeTableService = codeTableService;
  }

  public List<FieldError> validatePayload(ChallengeReportsSession session) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();

    if (!EnumUtils.isValidEnum(ChallengeReportsStatus.class, session.getChallengeReportsStatusCode())) {
      apiValidationErrors.add(ValidationUtil.createFieldError("challengeReportsSession", "challengeReportsStatusCode", session.getChallengeReportsStatusCode(), "Invalid challenge reports status provided."));
    }

    return apiValidationErrors;
  }
}
