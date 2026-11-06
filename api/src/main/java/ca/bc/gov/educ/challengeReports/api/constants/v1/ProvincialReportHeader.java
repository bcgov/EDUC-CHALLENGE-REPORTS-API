package ca.bc.gov.educ.challengeReports.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ProvincialReportHeader {

    MINCODE("MINCODE"),
    SCHOOL_NAME("SCHOOL NAME"),
    GROUP("GROUP"),
    PEN("PEN"),
    REPORT_DATE("REPORT DATE"),
    FUNDING_CODE("FUNDING CODE"),
    COURSE_CODE("COURSE CODE"),
    COURSE_LEVEL("COURSE LEVEL"),
    COURSE_SESSION("COURSE SESSION");

    private final String code;
    ProvincialReportHeader(String code) { this.code = code; }

    public static String[] getAllValuesAsStringArray(){
        return Arrays.stream(ProvincialReportHeader.values()).map(ProvincialReportHeader::getCode).toArray(String[]::new);
    }
}
