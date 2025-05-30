package ca.bc.gov.educ.challenge.reports.api.mappers.v1;

import ca.bc.gov.educ.challenge.reports.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.challenge.reports.api.mappers.UUIDMapper;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportsSessionEntity;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportsSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {LocalDateTimeMapper.class, UUIDMapper.class})
public interface ChallengeReportSessionMapper {

    ChallengeReportSessionMapper mapper = Mappers.getMapper(ChallengeReportSessionMapper.class);

    @Mapping(target = "schoolYear", source = "challengeReportsPeriod.schoolYear")
    ChallengeReportsSession toStructure(ChallengeReportsSessionEntity entity);

    ChallengeReportsSessionEntity toModel(ChallengeReportsSession entity);

}
