package ca.bc.gov.educ.challengeReports.api.rest;

import ca.bc.gov.educ.challengeReports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challengeReports.api.messaging.MessagePublisher;
import ca.bc.gov.educ.challengeReports.api.properties.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestUtilsTest {

  private final List<URI> requestedUris = Collections.synchronizedList(new ArrayList<>());
  private ApplicationProperties props;

  @BeforeEach
  void setUp() {
    requestedUris.clear();
    props = mock(ApplicationProperties.class);
    when(props.getGradStudentApiURL()).thenReturn("http://grad");
    when(props.getSdcApiURL()).thenReturn("http://sdc");
  }

  @Test
  void getChallengeReportGradStudentCoursesForYear_fetchesEveryPageSortedByStudentCourseID() throws Exception {
    var restUtils = restUtilsRespondingWith(uri -> uri.getQuery().contains("pageNumber=0")
            ? page("[{\"courseID\":\"A\"},{\"courseID\":\"B\"}]", 0, 2, 3)
            : page("[{\"courseID\":\"C\"}]", 1, 2, 3));

    var courses = restUtils.getChallengeReportGradStudentCoursesForYear(List.of("202407"));

    assertThat(courses).extracting("courseID").containsExactly("A", "B", "C");
    assertThat(requestedUris).hasSize(2);
    assertThat(requestedUris).allSatisfy(uri -> {
      var query = URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8);
      assertThat(uri.getPath()).isEqualTo("/grad/student/course/search/pagination");
      assertThat(query).contains("sort={\"studentCourseID\":\"ASC\"}");
    });
  }

  @Test
  void getChallengeReportGradStudentCoursesForYear_whenResponseIsEmpty_throws() {
    var restUtils = restUtilsRespondingWith(uri -> null);

    assertThatThrownBy(() -> restUtils.getChallengeReportGradStudentCoursesForYear(List.of("202407")))
            .isInstanceOf(ChallengeReportsAPIRuntimeException.class);
  }

  @Test
  void getSdcSchoolCollectionsForCollection_fetchesEveryPageForTheCollection() throws Exception {
    var restUtils = restUtilsRespondingWith(uri -> uri.getQuery().contains("pageNumber=0")
            ? page("[{\"sdcSchoolCollectionID\":\"1\",\"uploadReportDate\":\"20240929\"}]", 0, 1, 2)
            : page("[{\"sdcSchoolCollectionID\":\"2\",\"uploadReportDate\":\"20240930\"}]", 1, 1, 2));

    var collections = restUtils.getSdcSchoolCollectionsForCollection("abc-123");

    assertThat(collections).extracting("sdcSchoolCollectionID", "uploadReportDate")
            .containsExactly(tuple("1", "20240929"), tuple("2", "20240930"));
    assertThat(requestedUris).hasSize(2);
    assertThat(requestedUris).allSatisfy(uri -> {
      var query = URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8);
      assertThat(uri.getPath()).isEqualTo("/sdcSchoolCollection/paginated");
      assertThat(query).contains("sort={\"sdcSchoolCollectionID\":\"ASC\"}");
      assertThat(query).contains("\"key\":\"collectionEntity.collectionID\"").contains("\"value\":\"abc-123\"");
    });
  }

  @Test
  void get1701DataForStudents_fetchesEveryPageSortedBySdcSchoolCollectionStudentID() {
    var restUtils = restUtilsRespondingWith(uri -> uri.getQuery().contains("pageNumber=0")
            ? page("[{\"assignedStudentId\":\"s1\"}]", 0, 1, 2)
            : page("[{\"assignedStudentId\":\"s2\"}]", 1, 1, 2));

    var students = restUtils.get1701DataForStudents("abc-123", List.of("s1", "s2"));

    assertThat(students).extracting("assignedStudentId").containsExactly("s1", "s2");
    assertThat(requestedUris).hasSize(2);
    assertThat(requestedUris).allSatisfy(uri -> {
      var query = URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8);
      assertThat(uri.getPath()).isEqualTo("/sdcSchoolCollectionStudent/paginated-shallow");
      assertThat(query).contains("sort={\"sdcSchoolCollectionStudentID\":\"ASC\"}");
    });
  }

  @Test
  void getSdcSchoolCollectionsForCollection_whenResponseIsEmpty_throws() {
    var restUtils = restUtilsRespondingWith(uri -> null);

    assertThatThrownBy(() -> restUtils.getSdcSchoolCollectionsForCollection("abc-123"))
            .isInstanceOf(ChallengeReportsAPIRuntimeException.class);
  }

  private RestUtils restUtilsRespondingWith(Function<URI, String> bodyForUri) {
    var uriBuilderFactory = new DefaultUriBuilderFactory();
    uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
    var webClient = WebClient.builder()
            .uriBuilderFactory(uriBuilderFactory)
            .exchangeFunction(request -> {
              requestedUris.add(request.url());
              var body = bodyForUri.apply(request.url());
              var response = ClientResponse.create(HttpStatus.OK);
              if (body != null) {
                response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(body);
              }
              return Mono.just(response.build());
            })
            .build();
    return new RestUtils(webClient, webClient, props, mock(MessagePublisher.class));
  }

  private static String page(String content, int number, int size, long totalElements) {
    return "{\"content\":" + content + ",\"number\":" + number + ",\"size\":" + size + ",\"totalElements\":" + totalElements + "}";
  }
}
