package ca.bc.gov.educ.challenge.reports.api.controller.v1;

import ca.bc.gov.educ.challenge.reports.api.endpoint.v1.CodeTableAPIEndpoint;
import ca.bc.gov.educ.challenge.reports.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.challenge.reports.api.service.v1.CodeTableService;
import ca.bc.gov.educ.challenge.reports.api.struct.v1.ChallengeReportStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class CodeTableAPIController implements CodeTableAPIEndpoint {
    private final CodeTableService codeTableService;
    private static final CodeTableMapper mapper = CodeTableMapper.mapper;

    public CodeTableAPIController(CodeTableService codeTableService) {
        this.codeTableService = codeTableService;
    }

    @Override
    public List<ChallengeReportStatusCode> getChallengeReportStatusCodes() {
        return codeTableService.getAllChallengeReportStatusCodes().stream().map(mapper::toStructure).toList();
    }
}
