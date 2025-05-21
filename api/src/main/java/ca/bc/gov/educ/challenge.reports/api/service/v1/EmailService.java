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

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

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

        this.restUtils.sendEmail(emailData.getFromEmail(), emailData.getToEmails(), emailData.getBccEmails(), body, emailData.getSubject());
        log.debug("Email sent successfully");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendSampleEmailToStaff(String currentStage) {
        EmailData emailNotification;
        var activeSession = challengeReportsService.getChallengeReportActiveSession();
        if (currentStage.equalsIgnoreCase(NOT_STARTED.toString()) || currentStage.equalsIgnoreCase(PRELIM.toString())) {
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
                            "schoolYear", getYearWithNextValue(activeSession.getChallengeReportsPeriod().getSchoolYear()),
                            "finalDateForChanges", activeSession.getFinalDateForChanges() != null ? activeSession.getFinalDateForChanges().format(formatter) : "",
                            "executiveDirectorName", getBlankValueIfRequired(activeSession.getExecutiveDirectorName()),
                            "resourceManagementDirectorName", getBlankValueIfRequired(activeSession.getResourceManagementDirectorName())
                    ))
                    .build();
        } else {
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
                            "schoolYear", getYearWithNextValue(activeSession.getChallengeReportsPeriod().getSchoolYear()),
                            "finalDateForChanges", activeSession.getFinalDateForChanges() != null ? activeSession.getFinalDateForChanges().format(formatter) : "",
                            "executiveDirectorName", getBlankValueIfRequired(activeSession.getExecutiveDirectorName()),
                            "resourceManagementDirectorName", getBlankValueIfRequired(activeSession.getResourceManagementDirectorName()),
                            "preliminaryStageCompletionDate", activeSession.getPreliminaryStageCompletionDate() != null ? activeSession.getPreliminaryStageCompletionDate().format(formatter) : ""
                    ))
                    .build();
        }

        sendEmail(emailNotification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPreliminaryEmailToSupers() {
        EmailData emailNotification;
        var activeSession = challengeReportsService.getChallengeReportActiveSession();
        final var subject = emailProperties.getEmailSubjectPreliminaryToSuper();
        final var from = emailProperties.getEmailFromPreliminaryToSuper();
        final var to = emailProperties.getEmailToPreliminaryToSuper();
        var toEmails = getSuperintendentEmailAddressesForAllDistricts();

        emailNotification = EmailData.builder()
                .fromEmail(from)
                .toEmails(Collections.singletonList(to))
                .bccEmails(toEmails)
                .subject(subject)
                .templateName("preliminary.to.super")
                .emailFields(Map.of(
                        "fundingRate", getBlankValueIfRequired(activeSession.getFundingRate()),
                        "schoolYear", getYearWithNextValue(activeSession.getChallengeReportsPeriod().getSchoolYear()),
                        "finalDateForChanges", activeSession.getFinalDateForChanges() != null ? activeSession.getFinalDateForChanges().format(formatter) : "",
                        "executiveDirectorName", getBlankValueIfRequired(activeSession.getExecutiveDirectorName()),
                        "resourceManagementDirectorName", getBlankValueIfRequired(activeSession.getResourceManagementDirectorName())
                ))
                .build();

        sendEmail(emailNotification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendFinalEmailToSupers() {
        EmailData emailNotification;
        var activeSession = challengeReportsService.getChallengeReportActiveSession();
        final var subject = emailProperties.getEmailSubjectFinalToSuper();
        final var from = emailProperties.getEmailFromFinalToSuper();
        final var to = emailProperties.getEmailToFinalToSuper();
        var toEmails = getSuperintendentEmailAddressesForAllDistricts();

        emailNotification = EmailData.builder()
                .fromEmail(from)
                .toEmails(Collections.singletonList(to))
                .bccEmails(toEmails)
                .subject(subject)
                .templateName("final.to.super")
                .emailFields(Map.of(
                        "fundingRate", getBlankValueIfRequired(activeSession.getFundingRate()),
                        "schoolYear", getYearWithNextValue(activeSession.getChallengeReportsPeriod().getSchoolYear()),
                        "finalDateForChanges", activeSession.getFinalDateForChanges() != null ? activeSession.getFinalDateForChanges().format(formatter) : "",
                        "executiveDirectorName", getBlankValueIfRequired(activeSession.getExecutiveDirectorName()),
                        "resourceManagementDirectorName", getBlankValueIfRequired(activeSession.getResourceManagementDirectorName()),
                        "preliminaryStageCompletionDate", activeSession.getPreliminaryStageCompletionDate() != null ? activeSession.getPreliminaryStageCompletionDate().format(formatter) : ""
                ))
                .build();

        sendEmail(emailNotification);
    }

    public List<String> getSuperintendentEmailAddressesForAllDistricts(){
        var allDistrictUsers = restUtils.getAllEdxDistrictUsers();
        final List<String> emailSet = new ArrayList<>();
        allDistrictUsers.forEach(user ->
                user.getEdxUserDistricts().forEach(district ->
                        district.getEdxUserDistrictRoles().forEach(role -> {
                            if (Objects.equals(role.getEdxRoleCode(), "SUPERINT")) {
                                emailSet.add(user.getEmail());
                            }
                        })));
        return emailSet;
    }

    private String getBlankValueIfRequired(String s) {
        if (StringUtils.isBlank(s)) {
            return "";
        }

        return s;
    }

    private String getYearWithNextValue(String schoolYear) {
        if (StringUtils.isBlank(schoolYear)) {
            return "";
        }

        var year = Year.of(Integer.parseInt(schoolYear));

        return schoolYear + "/" + year.plusYears(1);
    }


}
