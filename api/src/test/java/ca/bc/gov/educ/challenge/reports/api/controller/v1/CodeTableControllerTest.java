package ca.bc.gov.educ.challenge.reports.api.controller.v1;

import ca.bc.gov.educ.challenge.reports.api.BaseChallengeReportsAPITest;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.URL;
import ca.bc.gov.educ.challenge.reports.api.repository.v1.ChallengeReportStatusCodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class CodeTableControllerTest extends BaseChallengeReportsAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  ChallengeReportStatusCodeRepository challengeReportStatusCodeRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    challengeReportStatusCodeRepository.save(createChallengeReportStatusCodeEntity());
  }

  @AfterEach
  public void tearDown() {
    challengeReportStatusCodeRepository.deleteAll();
  }

  protected static final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Test
  void testGetAllChallengeReportStatusCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_CHALLENGE_REPORTS_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.CHALLENGE_REPORT_STATUS_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].challengeReportStatusCode").value("PRELIM"));
  }

}
