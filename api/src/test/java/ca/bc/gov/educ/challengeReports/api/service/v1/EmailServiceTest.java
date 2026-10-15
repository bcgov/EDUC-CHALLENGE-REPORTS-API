package ca.bc.gov.educ.challengeReports.api.service.v1;

import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challengeReports.api.properties.EmailProperties;
import ca.bc.gov.educ.challengeReports.api.struct.v1.EmailData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"preliminary", "final", "preliminaryTeam", "finalTeam", "independent", "finance"})
    void sendsConfiguredMailboxesWithoutBlanksOrDuplicatesAndPreservesBcc(String notification) {
        var properties = mock(EmailProperties.class);
        var reports = mock(ChallengeReportsService.class);
        var session = mock(ChallengeReportsSessionEntity.class, RETURNS_DEEP_STUBS);
        when(reports.getChallengeReportActiveSession()).thenReturn(session);
        when(session.getChallengeReportsPeriod().getSchoolYear()).thenReturn("2026");
        when(session.getFundingRate()).thenReturn("$279.00");
        when(session.getFinalDateForChanges()).thenReturn(null);
        when(session.getPreliminaryStageCompletionDate()).thenReturn(null);
        String recipients = "Student.Certification@gov.bc.ca, , GRAD.Support@gov.bc.ca,Data.Management@gov.bc.ca,GRAD.Support@gov.bc.ca";
        when(properties.getEmailToPreliminaryToSuper()).thenReturn(recipients);
        when(properties.getEmailToFinalToSuper()).thenReturn(recipients);
        when(properties.getEmailToPrelimToStudentCertTeam()).thenReturn(recipients);
        when(properties.getEmailToFinalToStudentCertTeam()).thenReturn(recipients);
        when(properties.getEmailToFinalToIndyTeam()).thenReturn(recipients);
        when(properties.getEmailToFinalToPublicTeam()).thenReturn(recipients);
        var service = spy(new EmailService(null, Map.of(), null, properties, reports));
        var districtRecipients = List.of("district.admin@example.com");
        doReturn(districtRecipients).when(service).get1701AdminEmailAddressesForAllDistricts();
        doNothing().when(service).sendEmail(any());

        switch (notification) {
            case "preliminary" -> service.sendPreliminaryEmailTo1701Admin();
            case "final" -> service.sendFinalEmailTo1701Admin();
            case "preliminaryTeam" -> service.sendPreliminaryEmailToStudentCertTeam();
            case "finalTeam" -> service.sendFinalEmailToStudentCertTeam();
            case "independent" -> service.sendFinalEmailToIndySchoolsTeam();
            case "finance" -> service.sendFinalEmailToPublicFinanceSchoolsTeam();
            default -> throw new IllegalArgumentException(notification);
        }

        var email = ArgumentCaptor.forClass(EmailData.class);
        verify(service).sendEmail(email.capture());
        assertEquals(List.of("Student.Certification@gov.bc.ca", "GRAD.Support@gov.bc.ca", "Data.Management@gov.bc.ca"), email.getValue().getToEmails());
        if (notification.equals("independent") || notification.equals("finance")) {
            assertNull(email.getValue().getBccEmails());
        } else {
            assertEquals(districtRecipients, email.getValue().getBccEmails());
        }
    }
}
