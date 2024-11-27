package com.crc2jasper.jiraK2DataFetching;

import java.util.*;

public class CRInfo {
                        // e.g. PTF-85
    private final Map<String, List<CRRelationship>> allCRInfo = new LinkedHashMap<>();
    private static final CRInfo crInfo = new CRInfo();
    public static CRInfo getInstance(){return crInfo;}

    private class CRRelationship{
        // e.g. has to be done after, relates to, etc.
        private Set<String> allRelationshipSet = new LinkedHashSet<>();
        // e.g. PTF-90
        private String endingCR;

        public String getEndingCR(){
            return endingCR;
        }

        public Set<String> getAllRelationshipSet(){
            return allRelationshipSet;
        }
    }

    public void clearAllCRInfo(){
        allCRInfo.clear();
    }

    public Set<String> getAllBeginningCRs(){
        return allCRInfo.keySet();
    }

    public void addCRRelationship(String startingCR, String endingCR, String relationship){
        if (allCRInfo.containsKey(startingCR)){
            List<CRRelationship> crRelationships = allCRInfo.get(startingCR);
            for (CRRelationship crRelationship: crRelationships){
                if (crRelationship.endingCR.equals(endingCR)){
                    crRelationship.allRelationshipSet.add(relationship);
                    return;
                }
            }
            CRRelationship newCRRelationship = new CRRelationship();
            newCRRelationship.endingCR = endingCR;
            newCRRelationship.allRelationshipSet.add(relationship);
            crRelationships.add(newCRRelationship);
        }else{
            CRRelationship newCRRelationship = new CRRelationship();
            newCRRelationship.endingCR = endingCR;
            newCRRelationship.allRelationshipSet.add(relationship);
            List<CRRelationship> crRelationships = new ArrayList<>();
            crRelationships.add(newCRRelationship);
            allCRInfo.put(startingCR, crRelationships);
        }
    }

    public List<CRRelationship> getCRRelationshipSet(String startingCR){
        return allCRInfo.get(startingCR);
    }

    public static String compileAllCrInfo(List<String> rawCrInfo){
        /* Raw CR Info Example:
        * PTF-84 has to be done after PTF-90
        * PTF-85 has to be done after PTF-90
        * PTF-85 relates to PTF-90
        * PTF-85 relates to ECHART-29
        * */

        for (String info: rawCrInfo){
            String[] parts = info.split("\\s+");
            // ["PTF-84", "has", "to", "be", "done", "after", "PTF-90"]
            int len = parts.length; //e.g. 7
            String startingCR = parts[0];
            String endingCR = parts[len - 1];
            String relationship = String.join(" ", Arrays.copyOfRange(parts, 1, len - 1));
            crInfo.addCRRelationship(startingCR, endingCR, relationship);
        }
        List<String> concatenatedResults = new ArrayList<>();
        for (String key: crInfo.getAllBeginningCRs()){
            List<CRRelationship> crRelationships = crInfo.getCRRelationshipSet(key);
            for (CRRelationship crRelationship: crRelationships){
                String summary = APIQueryService.fetchCrLinkedSummary(crRelationship.endingCR);
                String endingCrWithSummary = summary.isBlank()? crRelationship.endingCR : crRelationship.endingCR + "_" + summary;
                String relationships = String.join(", ", crRelationship.allRelationshipSet);
                String combined = key + " " + relationships + " " + endingCrWithSummary;
                concatenatedResults.add(combined);
            }
        }
        crInfo.clearAllCRInfo();
        return String.join("; ", concatenatedResults);
    }

}
