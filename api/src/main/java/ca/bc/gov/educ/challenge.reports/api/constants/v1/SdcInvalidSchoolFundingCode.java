package ca.bc.gov.educ.challenge.reports.api.constants.v1;

import lombok.Getter;

@Getter
public enum SdcInvalidSchoolFundingCode {
    CODE_14("14"),
    CODE_20("20");

    private final String code;

    SdcInvalidSchoolFundingCode(final String code) {
        this.code = code;
    }

    public static String[] getSdcInvalidSchoolFundingCode(){
        return new String[]{CODE_14.getCode(), CODE_20.getCode()};
    }

}
