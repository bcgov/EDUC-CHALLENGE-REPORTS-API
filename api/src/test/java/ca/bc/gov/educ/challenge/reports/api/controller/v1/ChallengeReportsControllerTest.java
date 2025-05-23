package ca.bc.gov.educ.challenge.reports.api.controller.v1;

import ca.bc.gov.educ.challenge.reports.api.BaseChallengeReportsAPITest;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.URL;
import ca.bc.gov.educ.challenge.reports.api.mappers.v1.ChallengeReportSessionMapper;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsReportingPeriodEntity;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportStatusCodeRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsReportingPeriodRepository;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportsSessionRepository;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsSession;
import ca.bc.gov.educ.challenge.reports.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class ChallengeReportsControllerTest extends BaseChallengeReportsAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  ChallengeReportsReportingPeriodRepository challengeReportsReportingPeriodRepository;
  @Autowired
  ChallengeReportsSessionRepository challengeReportsSessionRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDown() {
    challengeReportsSessionRepository.deleteAll();
    challengeReportsReportingPeriodRepository.deleteAll();
  }

  protected static final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Test
  void testUpdateSession_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_CHALLENGE_REPORTS";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    ChallengeReportsReportingPeriodEntity reportingPeriod = new ChallengeReportsReportingPeriodEntity();
    reportingPeriod.setSchoolYear("2024");
    reportingPeriod.setActiveFromDate(LocalDateTime.now().minusDays(1));
    reportingPeriod.setActiveToDate(LocalDateTime.now().plusDays(1));
    reportingPeriod.setCreateUser("ABC");
    reportingPeriod.setCreateDate(LocalDateTime.now());
    reportingPeriod.setUpdateUser("ABC");
    reportingPeriod.setUpdateDate(LocalDateTime.now());

    reportingPeriod = challengeReportsReportingPeriodRepository.save(reportingPeriod);

    ChallengeReportsSessionEntity session = new ChallengeReportsSessionEntity();
    session.setChallengeReportsPeriod(reportingPeriod);
    session.setChallengeReportsStatusCode("PRELIM");
    session.setFundingRate("234");
    session.setFinalDateForChanges(LocalDateTime.now());
    session.setCreateUser("ABC");
    session.setCreateDate(LocalDateTime.now());
    session.setUpdateUser("ABC");
    session.setUpdateDate(LocalDateTime.now());

    session = challengeReportsSessionRepository.save(session);

    var structSession = ChallengeReportSessionMapper.mapper.toStructure(session);
    structSession.setCreateDate(null);
    structSession.setUpdateDate(null);
    var resultActions1 = this.mockMvc.perform(put(URL.BASE_URL + "/" + session.getChallengeReportsSessionID()).with(mockAuthority).content(JsonUtil.getJsonStringFromObject(structSession)).contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<ChallengeReportsSession>() {
    });

    assertThat(summary1).isNotNull();
  }

}
