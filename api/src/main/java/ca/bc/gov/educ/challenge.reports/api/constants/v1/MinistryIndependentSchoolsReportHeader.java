package ca.bc.gov.educ.challenge.reports.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum MinistryIndependentSchoolsReportHeader {

    SCHOOL_CODE("Mincode"),
    SCHOOL_NAME("School Name"),
    FUNDING_GROUP("Funding Group"),
    PEN("PEN"),
    COURSE_CODE_AND_LEVEL("Course Code and Level");

    private final String code;
    MinistryIndependentSchoolsReportHeader(String code) { this.code = code; }

    public static String[] getAllValuesAsStringArray(){
        return Arrays.stream(MinistryIndependentSchoolsReportHeader.values()).map(MinistryIndependentSchoolsReportHeader::getCode).toArray(String[]::new);
    }
}
