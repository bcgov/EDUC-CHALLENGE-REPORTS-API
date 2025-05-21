package ca.bc.gov.educ.challenge.reports.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchCriteriaBuilder {

    // Method to create the search criteria for SEPTEMBER collections from last year
    public static List<Map<String, Object>> septemberCollectionsFromLastYear(String processingYear) {
        List<Map<String, Object>> searchCriteriaList = new ArrayList<>();

        Map<String, Object> collectionTypeCodeCriteria = new HashMap<>();
        collectionTypeCodeCriteria.put("key", "collectionTypeCode");
        collectionTypeCodeCriteria.put("value", "SEPTEMBER");
        collectionTypeCodeCriteria.put("operation", "eq");
        collectionTypeCodeCriteria.put("valueType", "STRING");

        Map<String, Object> openDateCriteria = new HashMap<>();
        openDateCriteria.put("key", "openDate");
        openDateCriteria.put("value", processingYear + "-01-01," + processingYear + "-12-31");  // Start of last year
        openDateCriteria.put("operation", "btn");
        openDateCriteria.put("valueType", "DATE");
        openDateCriteria.put("condition", "AND");  // Adding condition for this item

        // Wrap it with a condition (AND for the second group)
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("condition", "AND");  // AND between conditions
        wrapper.put("searchCriteriaList", List.of(collectionTypeCodeCriteria, openDateCriteria));
        searchCriteriaList.add(wrapper);

        // Return the entire list of search criteria
        return searchCriteriaList;
    }


    public static List<Map<String, Object>> getChallengeReportGradCriteria(List<String> courseSessions) {
        List<Map<String, Object>> searchCriteriaList = new ArrayList<>();

        Map<String, Object> collectionIdCriteria = new HashMap<>();
        collectionIdCriteria.put("key", "courseSession");
        collectionIdCriteria.put("value", String.join(",", courseSessions));
        collectionIdCriteria.put("operation", "in");
        collectionIdCriteria.put("valueType", "STRING");

        Map<String, Object> eqOrChallengeCrit = new HashMap<>();
        eqOrChallengeCrit.put("key", "equivOrChallenge");
        eqOrChallengeCrit.put("operation", "eq");
        eqOrChallengeCrit.put("value", "C");
        eqOrChallengeCrit.put("valueType", "STRING");
        eqOrChallengeCrit.put("condition", "AND");

        Map<String, Object> finalPercentCrit = new HashMap<>();
        finalPercentCrit.put("key", "completedCoursePercentage");
        finalPercentCrit.put("operation", "gt");
        finalPercentCrit.put("value", "49");
        finalPercentCrit.put("valueType", "INTEGER");
        finalPercentCrit.put("condition", "AND");

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("condition", "AND"); // outer group condition
        wrapper.put("searchCriteriaList", List.of(collectionIdCriteria, eqOrChallengeCrit, finalPercentCrit));
        searchCriteriaList.add(wrapper);

        return searchCriteriaList;
    }

    public static List<Map<String, Object>> getSDCStudentsByCollectionIdAndStudentIDs(String collectionID, List<String> studentIDs) {
        List<Map<String, Object>> searchCriteriaList = new ArrayList<>();

        // First block: collection ID
        Map<String, Object> collectionIdCriteria = new HashMap<>();
        collectionIdCriteria.put("key", "sdcSchoolCollection.collectionEntity.collectionID");
        collectionIdCriteria.put("value", collectionID);
        collectionIdCriteria.put("operation", "eq");
        collectionIdCriteria.put("valueType", "UUID");

        // Second block: studentIDs (IN)
        Map<String, Object> pensCriteria = new HashMap<>();
        pensCriteria.put("key", "assignedStudentId");
        pensCriteria.put("operation", "in");
        pensCriteria.put("value", String.join(",", studentIDs));
        pensCriteria.put("valueType", "UUID");
        pensCriteria.put("condition", "AND"); // inside the group condition

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("condition", "AND"); // outer group condition
        wrapper.put("searchCriteriaList", List.of(collectionIdCriteria, pensCriteria));
        searchCriteriaList.add(wrapper);

        return searchCriteriaList;
    }


    public static List<Map<String, Object>> byCollectionIdAndFundingCode(String collectionID) {
        List<Map<String, Object>> searchCriteriaList = new ArrayList<>();

        // First block: collection ID
        Map<String, Object> collectionIdCriteria = new HashMap<>();
        collectionIdCriteria.put("key", "sdcSchoolCollection.collectionEntity.collectionID");
        collectionIdCriteria.put("value", collectionID);
        collectionIdCriteria.put("operation", "eq");
        collectionIdCriteria.put("valueType", "UUID");

        Map<String, Object> wrapper1 = new HashMap<>();
        wrapper1.put("condition", null); // first block, no outer condition
        wrapper1.put("searchCriteriaList", List.of(collectionIdCriteria));
        searchCriteriaList.add(wrapper1);

        // Second block: school funding code
        Map<String, Object> schoolFundingCriteria = new HashMap<>();
        schoolFundingCriteria.put("key", "schoolFundingCode");
        schoolFundingCriteria.put("value", "20");
        schoolFundingCriteria.put("operation", "eq");
        schoolFundingCriteria.put("valueType", "STRING");

        Map<String, Object> wrapper2 = new HashMap<>();
        wrapper2.put("condition", "AND"); // outer group condition
        wrapper2.put("searchCriteriaList", List.of(schoolFundingCriteria));
        searchCriteriaList.add(wrapper2);

        return searchCriteriaList;
    }

}


