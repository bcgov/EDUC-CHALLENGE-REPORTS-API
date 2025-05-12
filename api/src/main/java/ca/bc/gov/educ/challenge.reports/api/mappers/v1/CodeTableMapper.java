package ca.bc.gov.educ.challenge.reports.api.mappers.v1;

import ca.bc.gov.educ.challenge.reports.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.challenge.reports.api.model.v1.ChallengeReportStatusCodeEntity;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportStatusCode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {LocalDateTimeMapper.class})
public interface CodeTableMapper {

    CodeTableMapper mapper = Mappers.getMapper(CodeTableMapper.class);

    ChallengeReportStatusCode toStructure(ChallengeReportStatusCodeEntity entity);

}
