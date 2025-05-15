package ca.bc.gov.educ.challenge.reports.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DistrictReportHeader {

    SCHOOL_MINCODE("Mincode"),
    SCHOOL_NAME("School Name"),
    COURSE_SESSION("Course Session"),
    COURSE_CODE_AND_LEVEL("Course Code and Level"),
    PEN("PEN"),
    LEGAL_LAST_NAME("Legal Last Name"),
    LEGAL_FIRST_NAME("Legal Given Name"),
    LEGAL_MIDDLE_NAME("Legal Middle Name");

    private final String code;
    DistrictReportHeader(String code) { this.code = code; }

    public static String[] getAllValuesAsStringArray(){
        return Arrays.stream(DistrictReportHeader.values()).map(DistrictReportHeader::getCode).toArray(String[]::new);
    }
}
