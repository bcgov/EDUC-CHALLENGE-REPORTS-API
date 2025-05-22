package ca.bc.gov.educ.challenge.reports.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum MinistryReportTypeCode {
    DISTRICT_FUNDING_REPORT("DISTRICT_FUNDING_REPORT"),
    INDEPENDENT_SCHOOL_FUNDING_REPORT("INDEPENDENT_SCHOOL_FUNDING_REPORT");

    private final String code;
    MinistryReportTypeCode(String code) { this.code = code; }

    public static Optional<MinistryReportTypeCode> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
