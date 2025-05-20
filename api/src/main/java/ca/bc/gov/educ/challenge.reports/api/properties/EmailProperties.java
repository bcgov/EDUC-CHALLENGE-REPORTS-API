package ca.bc.gov.educ.challenge.reports.api.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class EmailProperties {
  @Value("${email.subject.preliminary.sample.staff}")
  private String emailSubjectPreliminarySampleStaff;

  @Value("${email.from.preliminary.sample.staff}")
  private String emailFromPreliminarySampleStaff;

  @Value("${email.to.preliminary.sample.staff}")
  private String emailToPreliminarySampleStaff;

  @Value("${email.subject.final.sample.staff}")
  private String emailSubjectFinalSampleStaff;

  @Value("${email.from.final.sample.staff}")
  private String emailFromFinalSampleStaff;

  @Value("${email.to.final.sample.staff}")
  private String emailToFinalSampleStaff;

}


