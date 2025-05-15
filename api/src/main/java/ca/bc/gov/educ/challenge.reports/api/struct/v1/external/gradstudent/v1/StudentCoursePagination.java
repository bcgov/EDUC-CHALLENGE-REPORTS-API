package ca.bc.gov.educ.challenge.reports.api.struct.v1.external.gradstudent.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentCoursePagination {

    private UUID studentCourseID;
    @NotBlank
    private String courseID;
    @NotBlank
    private String courseSession;
    private Integer finalPercent;
    private Integer credits;
    private String equivOrChallenge;
    private UUID studentExamId;

    private GraduationStudentPaginationRecord gradStudent;

}
