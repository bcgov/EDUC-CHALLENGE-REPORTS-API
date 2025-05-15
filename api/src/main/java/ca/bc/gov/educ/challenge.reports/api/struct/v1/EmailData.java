package ca.bc.gov.educ.challenge.reports.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailData {
  private static final long serialVersionUID = -2329245910142215178L;
  private String fromEmail;
  private List<String> toEmails;
  private String subject;
  private String templateName;
  private Map<String, String> emailFields;
}
