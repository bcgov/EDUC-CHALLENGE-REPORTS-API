package ca.bc.gov.educ.challenge.reports.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class FinalStageSagaData implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message="collectionID cannot be null.")
    private String challengeReportSessionID;

    @NotNull(message="updateUser cannot be null.")
    private String updateUser;

}
