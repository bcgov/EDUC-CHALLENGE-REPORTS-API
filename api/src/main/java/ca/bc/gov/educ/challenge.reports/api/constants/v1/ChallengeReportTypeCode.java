package ca.bc.gov.educ.challenge.reports.api.constants.v1;

import lombok.Getter;

@Getter
public enum ChallengeReportTypeCode {
    DISTRICT_REPORT("DISTRICT_REPORT");

    private final String code;
    ChallengeReportTypeCode(String code) { this.code = code; }

}
