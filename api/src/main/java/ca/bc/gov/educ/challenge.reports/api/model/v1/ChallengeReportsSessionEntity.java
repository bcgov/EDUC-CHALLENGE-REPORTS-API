package ca.bc.gov.educ.challenge.reports.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "CHALLENGE_REPORTS_SESSION")
public class ChallengeReportsSessionEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "CHALLENGE_REPORTS_SESSION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID challengeReportsSessionID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = ChallengeReportsReportingPeriodEntity.class)
  @JoinColumn(name = "CHALLENGE_REPORTS_PERIOD_ID", referencedColumnName = "CHALLENGE_REPORTS_PERIOD_ID", updatable = false)
  ChallengeReportsReportingPeriodEntity challengeReportsPeriod;

  @Column(name = "CHALLENGE_REPORTS_STATUS_CODE")
  private String challengeReportsStatusCode;

  @Column(name = "FUNDING_RATE")
  private String fundingRate;

  @Column(name = "FINAL_DATE_FOR_CHANGES")
  private LocalDateTime finalDateForChanges;

  @Column(name = "EXECUTIVE_DIRECTOR_NAME")
  private String executiveDirectorName;

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

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "challengeReportsSessionEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = ChallengeReportsPostedStudentEntity.class)
  Set<ChallengeReportsPostedStudentEntity> challengeReportsPostedStudentEntities;

}
