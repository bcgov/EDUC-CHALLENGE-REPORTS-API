package ca.bc.gov.educ.challenge.reports.api.rest;


import ca.bc.gov.educ.challenge.reports.api.constants.v1.EventType;
import ca.bc.gov.educ.challenge.reports.api.constants.v1.TopicsEnum;
import ca.bc.gov.educ.challenge.reports.api.exception.ChallengeReportsAPIRuntimeException;
import ca.bc.gov.educ.challenge.reports.api.messaging.MessagePublisher;
import ca.bc.gov.educ.challenge.reports.api.properties.ApplicationProperties;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.CHESEmail;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.Event;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.PaginatedResponse;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.coreg.v1.CourseCode;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.edx.v1.EdxUser;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.gradstudent.v1.StudentCoursePagination;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.District;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.sdc.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.studentapi.v1.Student;
import ca.bc.gov.educ.challenge.reports.api.util.JsonUtil;
import ca.bc.gov.educ.challenge.reports.api.util.SearchCriteriaBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.external.sdc.v1.Collection;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * This class is used for REST calls
 *
 */
@Component
@Slf4j
public class RestUtils {
  public static final String NATS_TIMEOUT = "Either NATS timed out or the response is null , correlationID :: ";
  private static final String CONTENT_TYPE = "Content-Type";
  private final Map<String, SchoolTombstone> schoolMap = new ConcurrentHashMap<>();
  private final Map<String, CourseCode> coregMap = new ConcurrentHashMap<>();
  private final Map<String, District> districtMap = new ConcurrentHashMap<>();
  private final Map<UUID, List<EdxUser>> edxDistrictUserMap = new ConcurrentHashMap<>();
  private final WebClient webClient;
  private final MessagePublisher messagePublisher;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ReadWriteLock edxUsersLock = new ReentrantReadWriteLock();
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();
  private final ReadWriteLock coregLock = new ReentrantReadWriteLock();
  private final ReadWriteLock districtLock = new ReentrantReadWriteLock();
  private final WebClient chesWebClient;
  @Getter
  private final ApplicationProperties props;

  @Value("${initialization.background.enabled}")
  private Boolean isBackgroundInitializationEnabled;

  private final Map<String, List<UUID>> independentAuthorityToSchoolIDMap = new ConcurrentHashMap<>();

  @Autowired
  public RestUtils(@Qualifier("chesWebClient") final WebClient chesWebClient, WebClient webClient, final ApplicationProperties props, final MessagePublisher messagePublisher) {
    this.webClient = webClient;
    this.props = props;
    this.chesWebClient = chesWebClient;
    this.messagePublisher = messagePublisher;
  }

  @PostConstruct
  public void init() {
    if (this.isBackgroundInitializationEnabled != null && this.isBackgroundInitializationEnabled) {
      ApplicationProperties.bgTask.execute(this::initialize);
    }
  }

  private void initialize() {
    this.populateSchoolMap();
    this.populateCoregMap();
    this.populateDistrictMap();
    this.populateEdxUsersMap();
  }

  @Scheduled(cron = "${schedule.jobs.load.school.cron}")
  public void scheduled() {
    this.init();
  }

  public void populateSchoolMap() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      for (val school : this.getSchools()) {
        this.schoolMap.put(school.getSchoolId(), school);
        if (StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
          this.independentAuthorityToSchoolIDMap.computeIfAbsent(school.getIndependentAuthorityId(), k -> new ArrayList<>()).add(UUID.fromString(school.getSchoolId()));
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} schools to memory", this.schoolMap.values().size());
  }

  public void populateEdxUsersMap() {
    val writeLock = this.edxUsersLock.writeLock();
    try {
      writeLock.lock();
      for (val edxUser : this.getEdxUsers()) {
        for(val edxUserDistrict: edxUser.getEdxUserDistricts()) {
          this.edxDistrictUserMap.computeIfAbsent(edxUserDistrict.getDistrictID(), k -> new ArrayList<>()).add(edxUser);
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache EDX users {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} EDX district users to memory", this.edxDistrictUserMap.values().size());
  }

  public void populateCoregMap() {
    val writeLock = this.coregLock.writeLock();
    try {
      writeLock.lock();
      for (val courseCode : this.getCoregCourses()) {
        this.coregMap.put(courseCode.getCourseID(), courseCode);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache coreg courses ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} coreg courses to memory", this.coregMap.values().size());
  }

  public List<EdxUser> getEdxUsers() {
    log.info("Calling EDX API to load EDX users to memory");
    return this.webClient.get()
            .uri(this.props.getEdxApiURL() + "/users")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(EdxUser.class)
            .collectList()
            .block();
  }

  public List<SchoolTombstone> getSchools() {
    log.info("Calling Institute api to load schools to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/school")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(SchoolTombstone.class)
            .collectList()
            .block();
  }

  public List<CourseCode> getCoregCourses() {
    log.info("Calling COREG API to load schools to memory");
    return this.webClient.get()
            .uri(this.props.getCoregApiURL() + "/all/39")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(CourseCode.class)
            .collectList()
            .block();
  }

  public void populateDistrictMap() {
    val writeLock = this.districtLock.writeLock();
    try {
      writeLock.lock();
      for (val district : this.getDistricts()) {
        this.districtMap.put(district.getDistrictId(), district);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache district ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} districts to memory", this.districtMap.values().size());
  }

  public List<District> getDistricts() {
    log.info("Calling Institute api to load districts to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/district")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(District.class)
            .collectList()
            .block();
  }

  public Optional<CourseCode> getCoregCourseByID(final String courseID) {
    if (this.coregMap.isEmpty()) {
      log.info("Coreg course map is empty reloading courses");
      this.populateCoregMap();
    }
    return Optional.ofNullable(this.coregMap.get(courseID));
  }

  public Optional<SchoolTombstone> getSchoolBySchoolID(final String schoolID) {
    if (this.schoolMap.isEmpty()) {
      log.info("School map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.schoolMap.get(schoolID));
  }

  public Optional<District> getDistrictByDistrictID(final String districtID) {
    if (this.districtMap.isEmpty()) {
      log.info("District map is empty reloading schools");
      this.populateDistrictMap();
    }
    return Optional.ofNullable(this.districtMap.get(districtID));
  }

  public PaginatedResponse<Collection> getLastSeptemberCollection(String processingYear) throws JsonProcessingException {
    List<Map<String, Object>> searchCriteriaList = SearchCriteriaBuilder.septemberCollectionsFromLastYear(processingYear);
    String searchJson = objectMapper.writeValueAsString(searchCriteriaList);
    String encodedSearchJson = URLEncoder.encode(searchJson, StandardCharsets.UTF_8);

    int pageNumber = 0;
    int pageSize = 50;

    try {
      String fullUrl = this.props.getSdcApiURL()
              + "/collection/paginated"
              + "?pageNumber=" + pageNumber
              + "&pageSize=" + pageSize
              + "&searchCriteriaList=" + encodedSearchJson;
      return webClient.get()
              .uri(fullUrl)
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .retrieve()
              .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<Collection>>() {
              })
              .block();
    } catch (Exception ex) {
      log.error("Error fetching schools on page {}", pageNumber, ex);
      return null;
    }
  }

  public List<EdxUser> getEdxUsersForDistrict(final UUID districtID) {
    if (this.edxDistrictUserMap.isEmpty()) {
      log.info("EDX users district map is empty reloading schools");
      this.populateEdxUsersMap();
    }
    var users = this.edxDistrictUserMap.get(districtID);
    return users != null ? users : new ArrayList<>();
  }

  public List<StudentCoursePagination> getChallengeReportGradStudentCoursesForYear(List<String> courseSessions) throws JsonProcessingException {
    int pageSize = 5000;
    int pageNumber = 0;

    var searchCriteriaList = SearchCriteriaBuilder.getChallengeReportGradCriteria(courseSessions);

    String searchJson = objectMapper.writeValueAsString(searchCriteriaList);
    String encodedSearchJson = URLEncoder.encode(searchJson, StandardCharsets.UTF_8);

    String fullUrl = this.props.getGradStudentApiURL()
            + "/grad/student/course/search/pagination"
            + "?pageNumber=" + pageNumber
            + "&pageSize=" + pageSize
            + "&sort="
            + "&searchCriteriaList=" + encodedSearchJson;

    PaginatedResponse<StudentCoursePagination> response = webClient.get()
            .uri(fullUrl)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<StudentCoursePagination>>() {
            })
            .block();
    return response.getContent();
  }

  private List<SdcSchoolCollectionStudent> fetchStudentsForBatch(int pageSize, List<Map<String, Object>> searchCriteriaList) throws JsonProcessingException {
    List<SdcSchoolCollectionStudent> students = new ArrayList<>();
    String searchJson = objectMapper.writeValueAsString(searchCriteriaList);
    String encodedSearchJson = URLEncoder.encode(searchJson, StandardCharsets.UTF_8);

    int pageNumber = 0;
    boolean hasNextPage = true;

    while (hasNextPage) {
      try {
        String fullUrl = this.props.getSdcApiURL()
                + "/sdcSchoolCollectionStudent/paginated-shallow"
                + "?pageNumber=" + pageNumber
                + "&pageSize=" + pageSize
                + "&sort=" // optional: add sort json or keep empty
                + "&searchCriteriaList=" + encodedSearchJson;

        PaginatedResponse<SdcSchoolCollectionStudent> response = webClient.get()
                .uri(fullUrl)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<SdcSchoolCollectionStudent>>() {
                })
                .block();

        if (response != null && response.getContent() != null) {
          students.addAll(response.getContent());
          hasNextPage = response.getNumber() < response.getTotalPages() - 1;
          pageNumber++;
        } else {
          hasNextPage = false;
        }
      } catch (Exception ex) {
        log.error("Error fetching 1701 data for page {} of batch starting at PEN {}", pageNumber, ex);
        break;
      }
    }

    return students;
  }

  public List<SdcSchoolCollectionStudent> get1701DataForStudents(String collectionID, List<String> studentIDs) {
    int maxPensPerBatch = 1500;
    int pageSize = 1500;

    ExecutorService executor = Executors.newFixedThreadPool(8); // Adjust thread pool size as needed
    List<CompletableFuture<List<SdcSchoolCollectionStudent>>> futures = new ArrayList<>();

    for (int i = 0; i < studentIDs.size(); i += maxPensPerBatch) {
      int start = i;
      int end = Math.min(i + maxPensPerBatch, studentIDs.size());
      List<String> students = new ArrayList<>(studentIDs.subList(start, end));

      CompletableFuture<List<SdcSchoolCollectionStudent>> future = CompletableFuture.supplyAsync(() -> {
        try {
          List<Map<String, Object>> searchCriteriaList = SearchCriteriaBuilder.getSDCStudentsByCollectionIdAndStudentIDs(collectionID, students);
          return fetchStudentsForBatch(pageSize, searchCriteriaList);
        } catch (Exception e) {
          log.error("Batch fetch failed", e);
          return Collections.emptyList();
        }
      }, executor);

      futures.add(future);
    }

    List<SdcSchoolCollectionStudent> allStudents = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());

    executor.shutdown();
    return allStudents;
  }

  public List<Student> getStudents(UUID correlationID, Set<String> studentIDs) {
    try {
      final TypeReference<Event> refEventResponse = new TypeReference<>() {};
      final TypeReference<List<Student>> refStudentResponse = new TypeReference<>() {};
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.GET_STUDENTS).eventPayload(objectMapper.writeValueAsString(studentIDs)).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.STUDENT_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage == null) {
        log.error("Received null response from GET STUDENTS for correlationID: {}", correlationID);
        throw new ChallengeReportsAPIRuntimeException(NATS_TIMEOUT + correlationID);
      } else {
        val eventResponse = objectMapper.readValue(responseMessage.getData(), refEventResponse);
        return objectMapper.readValue(eventResponse.getEventPayload(), refStudentResponse);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling GET STUDENTS service :: " + ex.getMessage());
      Thread.currentThread().interrupt();
      throw new ChallengeReportsAPIRuntimeException(ex.getMessage());
    }
  }

  public void sendEmail(final String fromEmail, final List<String> toEmail, final String body, final String subject) {
    this.sendEmail(this.getChesEmail(fromEmail, toEmail, body, subject));
  }

  private void sendEmail(final CHESEmail chesEmail) {
    this.chesWebClient
            .post()
            .uri(this.props.getChesEndpointURL())
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(Mono.just(chesEmail), CHESEmail.class)
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> this.logError(error, chesEmail))
            .doOnSuccess(success -> this.onSendEmailSuccess(success, chesEmail))
            .block();
  }

  public CHESEmail getChesEmail(final String fromEmail, final List<String> toEmail, final String body, final String subject) {
    final CHESEmail chesEmail = new CHESEmail();
    chesEmail.setBody(body);
    chesEmail.setBodyType("html");
    chesEmail.setDelayTS(0);
    chesEmail.setEncoding("utf-8");
    chesEmail.setFrom(fromEmail);
    chesEmail.setPriority("normal");
    chesEmail.setSubject(subject);
    chesEmail.setTag("tag");
    chesEmail.getTo().addAll(toEmail);
    return chesEmail;
  }

  private void logError(final Throwable throwable, final CHESEmail chesEmailEntity) {
    log.error("Error from CHES API call :: {} ", chesEmailEntity, throwable);
  }

  private void onSendEmailSuccess(final String s, final CHESEmail chesEmailEntity) {
    log.info("Email sent success :: {} :: {}", chesEmailEntity, s);
  }
}
