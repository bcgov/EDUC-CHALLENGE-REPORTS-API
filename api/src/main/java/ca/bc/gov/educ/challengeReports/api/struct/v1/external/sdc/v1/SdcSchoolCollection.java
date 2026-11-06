package ca.bc.gov.educ.challengeReports.api.struct.v1.external.sdc.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolCollection implements Serializable {

  private static final long serialVersionUID = 1L;

  private String sdcSchoolCollectionID;

  private String collectionID;

  private String schoolID;

  private String uploadReportDate;

}
