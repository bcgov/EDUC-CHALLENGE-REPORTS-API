package ca.bc.gov.educ.challenge.reports.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "CHALLENGE_REPORTS_PERIOD")
public class ChallengeReportsReportingPeriodEntity {
  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "CHALLENGE_REPORTS_PERIOD_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID challengeReportsPeriodID;

  @Basic
  @Column(name = "SCHOOL_YEAR")
  private String schoolYear;

  @Column(name = "ACTIVE_FROM_DATE", updatable = false)
  LocalDateTime activeFromDate;

  @Column(name = "ACTIVE_TO_DATE", updatable = false)
  LocalDateTime activeToDate;

  @Column(name = "CREATE_USER", updatable = false)
  String createUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @Column(name = "update_user")
  String updateUser;

  @PastOrPresent
  @Column(name = "update_date")
  LocalDateTime updateDate;

}
