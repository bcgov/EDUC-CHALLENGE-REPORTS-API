package ca.bc.gov.educ.challenge.reports.api.service.v1;

import ca.bc.gov.educ.challenge.reports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challenge.reports.api.rest.RestUtils;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.EmailData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@Slf4j
public class EmailService {

  private final SpringTemplateEngine templateEngine;
  private final Map<String, String> templateConfig;
  private final RestUtils restUtils;

  public EmailService(final SpringTemplateEngine templateEngine, final Map<String, String> templateConfig, RestUtils restUtils) {
    this.templateEngine = templateEngine;
    this.templateConfig = templateConfig;
    this.restUtils = restUtils;
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

}
