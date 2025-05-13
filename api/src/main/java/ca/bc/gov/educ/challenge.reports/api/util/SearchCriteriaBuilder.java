package ca.bc.gov.educ.challenge.reports.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchCriteriaBuilder {

    // Method to create the search criteria for SEPTEMBER collections from last year
    public static List<Map<String, Object>> septemberCollectionsFromLastYear(String processingYear) {
        List<Map<String, Object>> searchCriteriaList = new ArrayList<>();

        // Get the last year (e.g., 2024)
        //int lastYear = java.time.Year.now().getValue() - 1;

        // First search criteria for collectionTypeCode (SEPTEMBER)
        Map<String, Object> collectionTypeCodeCriteria = new HashMap<>();
        collectionTypeCodeCriteria.put("key", "collectionTypeCode");
        collectionTypeCodeCriteria.put("value", "SEPTEMBER");
        collectionTypeCodeCriteria.put("operation", "eq");
        collectionTypeCodeCriteria.put("valueType", "STRING");

        // Wrap it with a condition (null for the first group)
        Map<String, Object> wrapper1 = new HashMap<>();
        wrapper1.put("condition", null);  // No condition for the first group
        wrapper1.put("searchCriteriaList", List.of(collectionTypeCodeCriteria));
        searchCriteriaList.add(wrapper1);

        // Second search criteria for openDate (>= 2024-01-01)
        Map<String, Object> openDateCriteria = new HashMap<>();
        openDateCriteria.put("key", "openDate");
        openDateCriteria.put("value", processingYear + "-01-01");  // Start of last year
        openDateCriteria.put("operation", "gte");
        openDateCriteria.put("valueType", "DATE");
        openDateCriteria.put("condition", "AND");  // Adding condition for this item

        // Wrap it with a condition (AND for the second group)
        Map<String, Object> wrapper2 = new HashMap<>();
        wrapper2.put("condition", "AND");  // AND between conditions
        wrapper2.put("searchCriteriaList", List.of(openDateCriteria));
        searchCriteriaList.add(wrapper2);

        // Return the entire list of search criteria
        return searchCriteriaList;
    }

    public static List<Map<String, Object>> byCollectionIdAndStudentPens(String collectionID, List<String> studentPens) {
        List<Map<String, Object>> searchCriteriaList = new ArrayList<>();

        // First block: collection ID
        Map<String, Object> collectionIdCriteria = new HashMap<>();
        collectionIdCriteria.put("key", "sdcSchoolCollection.collectionEntity.collectionID");
        collectionIdCriteria.put("value", collectionID);
        collectionIdCriteria.put("operation", "eq");
        collectionIdCriteria.put("valueType", "UUID");

        Map<String, Object> wrapper1 = new HashMap<>();
        wrapper1.put("condition", null); // first block has no outer condition
        wrapper1.put("searchCriteriaList", List.of(collectionIdCriteria));
        searchCriteriaList.add(wrapper1);

        // Second block: studentPens (IN)
        Map<String, Object> pensCriteria = new HashMap<>();
        pensCriteria.put("key", "assignedPen");
        pensCriteria.put("operation", "in");
        pensCriteria.put("value", String.join(",", studentPens));
        pensCriteria.put("valueType", "STRING");
        pensCriteria.put("condition", "AND"); // inside the group condition

        Map<String, Object> wrapper2 = new HashMap<>();
        wrapper2.put("condition", "AND"); // outer group condition
        wrapper2.put("searchCriteriaList", List.of(pensCriteria));
        searchCriteriaList.add(wrapper2);

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


