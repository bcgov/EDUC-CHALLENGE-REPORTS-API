package ca.bc.gov.educ.challenge.reports.api.struct.v1;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.EventOutcome;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.EventType;
import lombok.Data;

@Data
public class ChoreographedEvent {
    /**
     * The Event id.
     */
    String eventID; // the primary key of student event table.
    /**
     * The Event type.
     */
    EventType eventType;
    /**
     * The Event outcome.
     */
    EventOutcome eventOutcome;
    /**
     * The Event payload.
     */
    String eventPayload;
    /**
     * The Create user.
     */
    String createUser;
    /**
     * The Update user.
     */
    String updateUser;
}
