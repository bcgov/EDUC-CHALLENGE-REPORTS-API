package ca.bc.gov.educ.challenge.reports.api.service.v1;


import ca.bc.gov.educ.challenge.reports.api.struct.v1.DownloadableReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;



@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {


    private static final String DISTRICT_ID = "districtID";

    public DownloadableReportResponse generateChallengeReportForThisYear(UUID districtID) {
        //Get current reporting period/session
        //Determine if in prelim or final stage or no stage at all
        //Pull students from GRAD if in PRELIM stage
        //Callout to SDC for current(last) Sept collection
        //Callout to SDC for students found in GRAD
        //Callout to SDC for school funding groups in last September
        //Ween out students that have a specific funding code or if school is not fundable
        //We *might* need to grab the student name (maybe we can use the 1701 record name)
        //Generate the CSV with the data and respond
        //
        //
        //Pull students from POSTED table if in FINAL stage
        //Generate the CSV with them
        return null;
    }
}
