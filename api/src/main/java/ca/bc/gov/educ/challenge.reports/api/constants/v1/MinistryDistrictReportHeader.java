package ca.bc.gov.educ.challenge.reports.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum MinistryDistrictReportHeader {

    DISTRICT_NUMBER("District Number"),
    DISTRICT_NAME("District Name"),
    TOTAL_COUNT("Total Successful Course Challenges");

    private final String code;
    MinistryDistrictReportHeader(String code) { this.code = code; }

    public static String[] getAllValuesAsStringArray(){
        return Arrays.stream(MinistryDistrictReportHeader.values()).map(MinistryDistrictReportHeader::getCode).toArray(String[]::new);
    }
}
