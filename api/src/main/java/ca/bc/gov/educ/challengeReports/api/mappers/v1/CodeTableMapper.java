package ca.bc.gov.educ.challengeReports.api.mappers.v1;

import ca.bc.gov.educ.challengeReports.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.challengeReports.api.model.v1.ChallengeReportStatusCodeEntity;
import ca.bc.gov.educ.challengeReports.api.struct.v1.ChallengeReportStatusCode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {LocalDateTimeMapper.class})
public interface CodeTableMapper {

    CodeTableMapper mapper = Mappers.getMapper(CodeTableMapper.class);

    ChallengeReportStatusCode toStructure(ChallengeReportStatusCodeEntity entity);

}
