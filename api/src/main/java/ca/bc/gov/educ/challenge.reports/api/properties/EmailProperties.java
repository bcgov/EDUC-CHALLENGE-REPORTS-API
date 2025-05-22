package ca.bc.gov.educ.challenge.reports.api.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class EmailProperties {

  @Value("${email.subject.preliminary.to.super}")
  private String emailSubjectPreliminaryToSuper;

  @Value("${email.from.preliminary.to.super}")
  private String emailFromPreliminaryToSuper;

  @Value("${email.to.preliminary.to.super}")
  private String emailToPreliminaryToSuper;

  @Value("${email.subject.final.to.super")
  private String emailSubjectFinalToSuper;

  @Value("${email.from.final.to.super}")
  private String emailFromFinalToSuper;

  @Value("${email.to.final.to.super}")
  private String emailToFinalToSuper;

}


