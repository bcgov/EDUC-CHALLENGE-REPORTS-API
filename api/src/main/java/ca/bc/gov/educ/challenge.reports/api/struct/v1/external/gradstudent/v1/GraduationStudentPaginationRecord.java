package ca.bc.gov.educ.challenge.reports.api.struct.v1.external.gradstudent.v1;

import ca.bc.gov.educ.challenge.reports.api.struct.v1.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class GraduationStudentPaginationRecord extends BaseRequest {

    private String pen;
    private UUID schoolOfRecordId;
    private String studentGrade;
    private String studentStatus;
    private UUID studentID;
}
