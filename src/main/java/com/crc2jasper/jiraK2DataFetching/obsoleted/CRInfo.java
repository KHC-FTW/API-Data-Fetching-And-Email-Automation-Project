package com.crc2jasper.jiraK2DataFetching.obsoleted;

public class CRInfo {
    /* This class has been obsoleted. */
    /*                    // e.g. PTF-85
    private final Map<String, Set<String>> endingCrTicketRelationshipMap = new LinkedHashMap<>();
    private final Map<String, String> allSummaryToLinkedIssuesMap = new HashMap<>();
    private static final CRInfo crInfo = new CRInfo();
    public static CRInfo getInstance(){return crInfo;}

//    private class CRRelationship{
//
//        // e.g. done after, relates to, etc.
//        private Set<String> allRelationshipSet = new LinkedHashSet<>();
//        // e.g. PTF-90
//        private String endingCR;
//
//        public String getEndingCR(){
//            return endingCR;
//        }
//
//        public Set<String> getAllRelationshipSet(){
//            return allRelationshipSet;
//        }
//    }


    public Map<String, String> allSummaryToLinkedIssues(){
        return allSummaryToLinkedIssuesMap;
    }

    public static String getLinkedIssues(String formSummary){
        if (crInfo.allSummaryToLinkedIssuesMap.containsKey(formSummary)){
            return crInfo.allSummaryToLinkedIssuesMap.get(formSummary);
        }
        return "";
    }

    private void addRelationship(String endingCR, String relationship){
        if(endingCrTicketRelationshipMap.containsKey(endingCR)){
            endingCrTicketRelationshipMap.get(endingCR).add(relationship);
        }else{
            Set<String> relationshipSet = new LinkedHashSet<>();
            relationshipSet.add(relationship);
            endingCrTicketRelationshipMap.put(endingCR, relationshipSet);
        }
    }

    private void addFinalizedLinkedIssues(String formSummary, Map<String, String> endingCrSummaryMap, Map<String, Set<String>> endingCrTicketRelationshipMap){
        List<String> fullRelationships = new ArrayList<>();
        for (String endingCr: endingCrTicketRelationshipMap.keySet()){
            String relationshipString = String.join(" & ", endingCrTicketRelationshipMap.get(endingCr)) + " " + (endingCrSummaryMap.get(endingCr).isBlank() ? endingCr : endingCr + "_" + endingCrSummaryMap.get(endingCr));
            fullRelationships.add(relationshipString);
        }
        // after extracting relevant info, must clear the list
        // since the next crTicket has nothing to do with the current one
        clearEndingCrTicketRelationshipMap();
        allSummaryToLinkedIssuesMap.put(formSummary, String.join("; ", fullRelationships));
    }

    public void clearSummaryToLinkedIssues(){
        allSummaryToLinkedIssuesMap.clear();
    }

    public void clearEndingCrTicketRelationshipMap(){
        endingCrTicketRelationshipMap.clear();
    }

    public static void compileFinalizedCrInfo(List<String> allParentLinkedIssues, String formSummary){
        *//* Raw CR Info Example:
        * PTF-84 done after PTF-90
        * PTF-84 finished together with PTF-90
        * *//*

        // need a map for endingCR: endingSummary
        Map<String, String> endingCrSummaryMap = new HashMap<>();

        String endingCR = "";
        for (String info: allParentLinkedIssues){
            String[] parts = info.split("\\s+");
            // ["PTF-84", "done", "after", "PTF-90"]
            int len = parts.length; //e.g. 4
//            String startingCR = parts[0];
            endingCR = parts[len - 1];
            if(!endingCrSummaryMap.containsKey(endingCR)){
                String endingSummary = APIQueryService.fetchCrLinkedSummary(endingCR);
                if(!endingSummary.contains("_")){
                    endingSummary = "";
                }
                endingCrSummaryMap.put(endingCR, endingSummary);
            }
            String relationship = String.join(" ", Arrays.copyOfRange(parts, 1, len - 1));
            crInfo.addRelationship(endingCR, relationship);
        }
//        String endingSummary = APIQueryService.fetchCrLinkedSummary(endingCR);
        crInfo.addFinalizedLinkedIssues(formSummary, endingCrSummaryMap, crInfo.endingCrTicketRelationshipMap);
    }
*/
}
