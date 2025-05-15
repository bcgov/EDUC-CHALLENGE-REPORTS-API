package ca.bc.gov.educ.challenge.reports.api.struct.v1;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class ChallengeReportsSession extends BaseRequest{

    @NotNull(message = "challengeReportsSessionID can not be null.")
    private UUID challengeReportsSessionID;

    @Size(max = 10)
    private String challengeReportsStatusCode;

    @Size(max = 255)
    @NotNull(message = "fundingRate can not be null.")
    private String fundingRate;

    @NotNull(message = "finalDateForChanges can not be null.")
    private LocalDateTime finalDateForChanges;

    @Size(max = 255)
    @NotNull(message = "executiveDirectorName can not be null.")
    private String executiveDirectorName;

    @Size(max = 255)
    @NotNull(message = "resourceManagementDirectorName can not be null.")
    private String resourceManagementDirectorName;

}
