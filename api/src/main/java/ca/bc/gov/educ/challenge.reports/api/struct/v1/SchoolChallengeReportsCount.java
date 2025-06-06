package ca.bc.gov.educ.challenge.reports.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class SchoolChallengeReportsCount implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String schoolID;

  private String count;

}
