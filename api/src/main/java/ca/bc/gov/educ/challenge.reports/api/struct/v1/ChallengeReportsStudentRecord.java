package ca.bc.gov.educ.challenge.reports.api.struct.v1;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class ChallengeReportsStudentRecord {

    private UUID schoolID;

    private UUID districtID;

    private UUID studentID;

    private String pen;

    private String courseSession;

    private String courseCode;

    private String courseLevel;

    private String studentSurname;

    private String studentGivenName;

    private String studentMiddleNames;

}
