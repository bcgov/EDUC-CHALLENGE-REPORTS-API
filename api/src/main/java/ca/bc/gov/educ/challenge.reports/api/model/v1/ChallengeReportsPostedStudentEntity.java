package ca.bc.gov.educ.challenge.reports.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "CHALLENGE_REPORTS_POSTED_STUDENT")
public class ChallengeReportsPostedStudentEntity {
  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "CHALLENGE_REPORTS_POSTED_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID challengeReportsPostedStudentID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = ChallengeReportsSessionEntity.class)
  @JoinColumn(name = "CHALLENGE_REPORTS_SESSION_ID", referencedColumnName = "CHALLENGE_REPORTS_SESSION_ID", updatable = false)
  ChallengeReportsSessionEntity challengeReportsSessionEntity;

  @Basic
  @Column(name = "SCHOOL_ID")
  private UUID schoolID;

  @Basic
  @Column(name = "DISTRICT_ID")
  private UUID districtID;

  @Basic
  @Column(name = "STUDENT_ID")
  private UUID studentID;

  @Column(name = "PEN")
  private String pen;

  @Column(name = "COURSE_SESSION")
  private String courseSession;

  @Column(name = "COURSE_CODE")
  private String courseCode;

  @Column(name = "COURSE_LEVEL")
  private String courseLevel;

  @Column(name = "STUDENT_SURNAME")
  private String studentSurname;

  @Column(name = "STUDENT_GIVEN_NAME")
  private String studentGivenName;

  @Column(name = "STUDENT_MIDDLE_NAMES")
  private String studentMiddleNames;

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
