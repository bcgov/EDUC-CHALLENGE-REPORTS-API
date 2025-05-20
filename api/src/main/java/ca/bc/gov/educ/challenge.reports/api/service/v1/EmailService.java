package ca.bc.gov.educ.challenge.reports.api.service.v1;

import ca.bc.gov.educ.challenge.reports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challenge.reports.api.properties.EmailProperties;
import ca.bc.gov.educ.challenge.reports.api.rest.RestUtils;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.EmailData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

import static ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus.NOT_STARTED;
import static ca.bc.gov.educ.challenge.reports.api.constants.v1.ChallengeReportsStatus.PRELIM;

@Service
@Slf4j
public class EmailService {

  private final SpringTemplateEngine templateEngine;
  private final Map<String, String> templateConfig;
  private final RestUtils restUtils;
  private final EmailProperties emailProperties;
  private final ChallengeReportsService challengeReportsService;

  public EmailService(final SpringTemplateEngine templateEngine, final Map<String, String> templateConfig, RestUtils restUtils, EmailProperties emailProperties, ChallengeReportsService challengeReportsService) {
    this.templateEngine = templateEngine;
    this.templateConfig = templateConfig;
    this.restUtils = restUtils;
    this.emailProperties = emailProperties;
    this.challengeReportsService = challengeReportsService;
  }

  public void sendEmail(final EmailData emailData) {
    log.debug("Sending email");

    final var ctx = new Context();
    emailData.getEmailFields().forEach(ctx::setVariable);

    if (!this.templateConfig.containsKey(emailData.getTemplateName())) {
      throw new ChallengeReportsAPIRuntimeException("Email template not found for template name :: " + emailData.getTemplateName());
    }

    final var body = this.templateEngine.process(this.templateConfig.get(emailData.getTemplateName()), ctx);

    this.restUtils.sendEmail(emailData.getFromEmail(), emailData.getToEmails(), body, emailData.getSubject());
    log.debug("Email sent successfully");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void sendSampleEmailToStaff(String currentStage) {
    EmailData emailNotification;
    var activeSession = challengeReportsService.getChallengeReportActiveSession();
    if(currentStage.equalsIgnoreCase(NOT_STARTED.toString()) || currentStage.equalsIgnoreCase(PRELIM.toString())){
      final var subject = emailProperties.getEmailSubjectPreliminarySampleStaff();
      final var from = emailProperties.getEmailFromPreliminarySampleStaff();
      final var to = emailProperties.getEmailToPreliminarySampleStaff();

      emailNotification = EmailData.builder()
              .fromEmail(from)
              .toEmails(Collections.singletonList(to))
              .subject(subject)
              .templateName("preliminary.sample.staff")
              .emailFields(Map.of(
                      "fundingRate", getBlankValueIfRequired(activeSession.getFundingRate()),
                      "schoolYear", getBlankValueIfRequired(activeSession.getChallengeReportsPeriod().getSchoolYear()),
                      "finalDateForChanges", activeSession.getFinalDateForChanges() != null ? activeSession.getFinalDateForChanges().format(DateTimeFormatter.ISO_LOCAL_DATE) : "",
                      "executiveDirectorName", getBlankValueIfRequired(activeSession.getExecutiveDirectorName()),
                      "resourceManagementDirectorName", getBlankValueIfRequired(activeSession.getResourceManagementDirectorName())
              ))
              .build();
    }else{
      final var subject = emailProperties.getEmailSubjectFinalSampleStaff();
      final var from = emailProperties.getEmailFromFinalSampleStaff();
      final var to = emailProperties.getEmailToFinalSampleStaff();

      emailNotification = EmailData.builder()
              .fromEmail(from)
              .toEmails(Collections.singletonList(to))
              .subject(subject)
              .templateName("final.sample.staff")
              .emailFields(Map.of(
                      "fundingRate", getBlankValueIfRequired(activeSession.getFundingRate()),
                      "schoolYear", getBlankValueIfRequired(activeSession.getChallengeReportsPeriod().getSchoolYear()),
                      "finalDateForChanges", activeSession.getFinalDateForChanges() != null ? activeSession.getFinalDateForChanges().format(DateTimeFormatter.ISO_LOCAL_DATE) : "",
                      "executiveDirectorName", getBlankValueIfRequired(activeSession.getExecutiveDirectorName()),
                      "resourceManagementDirectorName", getBlankValueIfRequired(activeSession.getResourceManagementDirectorName()),
                      "preliminaryStageCompletionDate", activeSession.getPreliminaryStageCompletionDate() != null ? activeSession.getPreliminaryStageCompletionDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : ""
              ))
              .build();
    }

    sendEmail(emailNotification);
  }

  private String getBlankValueIfRequired(String s){
    if(StringUtils.isBlank(s)){
      return "";
    }

    return s;
  }


}
