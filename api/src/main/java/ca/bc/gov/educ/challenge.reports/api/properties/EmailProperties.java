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

  @Value("${email.subject.final.to.super}")
  private String emailSubjectFinalToSuper;

  @Value("${email.from.final.to.super}")
  private String emailFromFinalToSuper;

  @Value("${email.to.final.to.super}")
  private String emailToFinalToSuper;

  @Value("${email.subject.final.to.funding.indy.team}")
  private String emailSubjectFinalToIndyTeam;

  @Value("${email.from.final.to.funding.indy.team}")
  private String emailFromFinalToIndyTeam;

  @Value("${email.to.final.to.funding.indy.team}")
  private String emailToFinalToIndyTeam;

  @Value("${email.subject.final.to.funding.public.team}")
  private String emailSubjectFinalToPublicTeam;

  @Value("${email.from.final.to.funding.public.team}")
  private String emailFromFinalToPublicTeam;

  @Value("${email.to.final.to.funding.public.team}")
  private String emailToFinalToPublicTeam;

  @Value("${email.subject.final.to.student.cert.team}")
  private String emailSubjectFinalToStudentCertTeam;

  @Value("${email.from.final.to.student.cert.team}")
  private String emailFromFinalToStudentCertTeam;

  @Value("${email.to.final.to.student.cert.team}")
  private String emailToFinalToStudentCertTeam;

  @Value("${email.subject.preliminary.to.student.cert.team}")
  private String emailSubjectPrelimToStudentCertTeam;

  @Value("${email.from.preliminary.to.student.cert.team}")
  private String emailFromPrelimToStudentCertTeam;

  @Value("${email.to.preliminary.to.student.cert.team}")
  private String emailToPrelimToStudentCertTeam;
}


