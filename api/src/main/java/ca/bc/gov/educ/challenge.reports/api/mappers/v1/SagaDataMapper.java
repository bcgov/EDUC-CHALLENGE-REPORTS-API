package ca.bc.gov.educ.challenge.reports.api.mappers.v1;


import ca.bc.gov.educ.challenge.reports.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.challenge.reports.api.mappers.UUIDMapper;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSagaEntity;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.FinalStageSagaData;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.PreliminaryStageSagaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Saga mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface SagaDataMapper {
  /**
   * The constant mapper.
   */
  SagaDataMapper mapper = Mappers.getMapper(SagaDataMapper.class);

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "sagaState", ignore = true)
  @Mapping(target = "sagaId", ignore = true)
  @Mapping(target = "retryCount", ignore = true)
  @Mapping(target = "payload", expression = "java(ca.bc.gov.educ.challenge.reports.api.util.JsonUtil.getJsonStringFromObject(sagaData))")
  ChallengeReportsSagaEntity toModel(String sagaName, PreliminaryStageSagaData sagaData) throws JsonProcessingException;

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "sagaState", ignore = true)
  @Mapping(target = "sagaId", ignore = true)
  @Mapping(target = "retryCount", ignore = true)
  @Mapping(target = "payload", expression = "java(ca.bc.gov.educ.challenge.reports.api.util.JsonUtil.getJsonStringFromObject(sagaData))")
  ChallengeReportsSagaEntity toModel(String sagaName, FinalStageSagaData sagaData) throws JsonProcessingException;

}
