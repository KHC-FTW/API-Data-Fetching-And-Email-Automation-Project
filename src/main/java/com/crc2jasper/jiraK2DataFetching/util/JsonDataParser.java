package com.crc2jasper.jiraK2DataFetching.util;

import com.crc2jasper.jiraK2DataFetching.component.PromoForm;
import com.crc2jasper.jiraK2DataFetching.component.PromoReleaseEmailConfig;
import com.crc2jasper.jiraK2DataFetching.service.AppIniService;
import com.crc2jasper.jiraK2DataFetching.component.DataCenter;
import com.crc2jasper.jiraK2DataFetching.service.APIQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class JsonDataParser {
    private JsonDataParser(){}
    private static final ObjectMapper OBJECT_MAPPER = AppIniService.getObjectMapper();
    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = com.crc2jasper.jiraK2DataFetching.component.PromoReleaseEmailConfig.getInstance();
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();
    private static final String AFFECTED_HOSP_REGEX = "(Affected Hospital|Effective Date).|\\{color:.{0,8}}|\\{color}|\\\\u[0-9A-Fa-f]{4}\"|<[^>]*>|&[a-zA-Z0-9#]+;|[*{}]";

    private static boolean isImpHospOrImpCorp(List<String> allTypes){
        return allTypes.contains("imp-hosp-db") || allTypes.contains("imp-corp-db");
    }

    private static String extractPPMSummary(String ITOCMS_PPM){
        //e.g. ITOCMS-35975, PPM2024_U0237
        // or ITOCMS-35975
        String[] parts = ITOCMS_PPM.split(", ");
        for (String part: parts){
            if(part.contains("PPM")){
                return part;
            }
        }
        return ITOCMS_PPM;
    }

    public static Map<String, String> retrieveTicketSummaryAndRelatedTickets(String jiraResp){
        Map<String, String> ticketSummaryAndChildTickets = new HashMap<>();
        try {
            JsonNode issues = OBJECT_MAPPER.readTree(jiraResp).get("issues");
            for (JsonNode currIssue: issues){
                String key_ITOCMS = currIssue.get("key").asText();
                JsonNode fields = currIssue.get("fields");
                String summary = fields.get("summary").asText();
                String relatedTickets = fields.get("customfield_11599").asText();
                ticketSummaryAndChildTickets.put("key_ITOCMS", key_ITOCMS);
                ticketSummaryAndChildTickets.put("summary", summary);
                ticketSummaryAndChildTickets.put("relatedTickets", relatedTickets);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ticketSummaryAndChildTickets;
    }

    public static void parseJiraUrgentServiceForBiweeklyResp(String jiraResp){
        try {
            JsonNode issues = OBJECT_MAPPER.readTree(jiraResp).get("issues");
            for (JsonNode currIssue: issues){
                // "customfield_11400": "https://wfeng-svc/Runtime/Runtime/Form/CMS__Promotion__Form?formnumber=M-ITOCMS-24-1244"
                JsonNode fields = currIssue.get("fields");
                String k2FormLink = fields.get("customfield_11400").asText();
                String k2FormNo = retrieveK2FormNoFromLink(k2FormLink);
                // Here, with the form no. e.g. M-ITOCMS-24-1244, we have to fetch JFrog for the types
                String jFrogResp = APIQueryService.fetchJFrogAPIForTypes(k2FormNo);
                List<String> allTypePaths = retrieveTypePathsFromJFrogResp(jFrogResp);
                List<String> allTypes = retrieveFinalTypesFromTypePaths(allTypePaths);
                if(isImpHospOrImpCorp(allTypes)){
                    PromoForm promoForm = new PromoForm().k2FormLink(k2FormLink)
                            .k2FormNo(k2FormNo).types(allTypes);
                    String parentTicket = currIssue.get("key").asText();
                    Map<String, String> ticketSummaryAndRelatedTickets = APIQueryService.fetchTicketSummaryAndRelatedTickets(parentTicket);
                    String key_ITOCMS = ticketSummaryAndRelatedTickets.get("key_ITOCMS");
                    String summary = ticketSummaryAndRelatedTickets.get("summary");
                    String relatedTickets = ticketSummaryAndRelatedTickets.get("relatedTickets");

                    promoForm.key_ITOCMS(key_ITOCMS).summary(summary)
                            .allTickets(new ArrayList<>(Arrays.asList(relatedTickets.split(", "))));

                    Map<String, Set<String>> endingTicketRelationshipMap = processIssueLinks(fields.get("issuelinks"));

                    // in relatedTickets, check to see if it's only parent ticket or there are child tickets too
                    String[] allTickets = relatedTickets.split(", ");
                    if (allTickets.length > 1){
                        String[] childTickets = Arrays.copyOfRange(allTickets, 1, allTickets.length);
                        String response = APIQueryService.fetchTicketIssueLinks(String.join(",", childTickets));
                        JsonNode childTicketIssues = OBJECT_MAPPER.readTree(response).get("issues");
                        for (JsonNode childTicketIssue: childTicketIssues){
                            Map<String, Set<String>> childTicketRelationshipMap = processIssueLinks(childTicketIssue.get("fields").get("issuelinks"));
                            endingTicketRelationshipMap.putAll(childTicketRelationshipMap);
                        }
                    }
                    promoForm.endingTicketRelationshipMap(endingTicketRelationshipMap);

                    Map<String, String> tempEndingTicketSummaryMap = new HashMap<>();
                    for(String endingTicket: endingTicketRelationshipMap.keySet()){
                        String endingTicketSummary = APIQueryService.fetchTicketSummary(endingTicket);;
                        tempEndingTicketSummaryMap.put(endingTicket, endingTicketSummary);
                    }
                    promoForm.endingTicketSummaryMap(tempEndingTicketSummaryMap);

                    String affectedHosp = retrieveAffectedHosp(fields);
                    promoForm.affectedHosp(affectedHosp);
                    DATA_CENTER.addUrgentServiceForm(key_ITOCMS, promoForm);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception raised when fetching Jira Urgent/Service Promotions for Bi-weekly: " + e.getMessage() + "\n");
        }
    }

    public static void parseStandardJiraResp(String jiraResp, boolean isBiweekly){
        try{
            JsonNode issues = OBJECT_MAPPER.readTree(jiraResp).get("issues");
            for(JsonNode issue: issues){
                JsonNode fields = issue.get("fields");
                if(!isCurrentBatch(fields)) continue;
                String key_ITOCMS = issue.get("key").asText();
                String summary_PPM = fields.get("summary").asText();
                String targetDate = fields.get("customfield_11628").asText();
                String description = fields.get("description").asText();
                String status = fields.get("status").get("name").asText();
                PromoForm promoForm = new PromoForm().targetDate(targetDate).key_ITOCMS(key_ITOCMS)
                        .summary(summary_PPM).description(description).status(status);
                DATA_CENTER.addPromoForm(key_ITOCMS, promoForm);
                if(isBiweekly){
                    String allTicketString = fields.get("customfield_11599").asText();
                    String[] allTickets = allTicketString.split(", ");
                    promoForm.allTickets(new ArrayList<>(Arrays.asList(allTickets)));
                }
                APIQueryService.jiraTicketInfoFromITOCMSKey(key_ITOCMS, isBiweekly);
                List<String> allTypePaths = null;
                if(summary_PPM.contains("PPM")){
                    // fetch from jFrog
                    String k2FormNo = promoForm.getK2FormNo();
                    String jFrogResp = APIQueryService.fetchJFrogAPIForTypes(k2FormNo);
                    allTypePaths = retrieveTypePathsFromJFrogResp(jFrogResp);
                }else{
                    String rawK2FormLink = fields.get("customfield_11400").asText();
                    String k2FormLink = "", k2FormNo = "N/A";
                    if (!rawK2FormLink.equalsIgnoreCase("null")){
                        k2FormLink = retrieveK2FormLink(rawK2FormLink);
                        k2FormNo = retrieveK2FormNoFromLink(k2FormLink);
                    }
                    promoForm.k2FormLink(k2FormLink).k2FormNo(k2FormNo);
                    // from customfield_14500
                    String cd_configuration = fields.get("customfield_14500").asText();
                    if (!cd_configuration.equalsIgnoreCase("null")){
                        allTypePaths = retrieveTypePathsfromCF14500(cd_configuration);
                    }else{
                        // from http://cdrasvn:90/
                        String parentTicket = promoForm.getAllTickets().getFirst();
                        allTypePaths = APIQueryService.collabNetInitialAPI(parentTicket);
                    }
                }
                List<String> allTypes = retrieveFinalTypesFromTypePaths(allTypePaths);
                promoForm.types(allTypes);
                promoForm.isImpHospOrImpCorp(isImpHospOrImpCorp(allTypes));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static List<String> retrieveFinalTypesFromTypePaths(List<String> allTypePaths) {
        Map<Integer, String> sequenceTypeMap = new TreeMap<>();
        for (String path: allTypePaths){
            // String[] type:
            // index 0: sequence no. (still as string, not parsed to int yet)
            // index 1: actual type e.g. imp-hosp-db, imp-corp-db
            String[] seqAndType = extractSeqAndTypeFromPath(path);
            sequenceTypeMap.put(Integer.parseInt(seqAndType[0]), seqAndType[1]);
        }
        return new ArrayList<>(sequenceTypeMap.values());
    }

    private static String[] extractSeqAndTypeFromPath(String pathPart){
        try{
            String[] parts = pathPart.split("_");
            return new String[]{parts[1], parts[2]};
        }catch (Exception e){
            System.out.println("Error occurred in extractTypeFromPath().");
            e.printStackTrace();
            return new String[]{"-1", "ERROR"};
        }
    }

    private static List<String> retrieveTypePathsfromCF14500(String cd_configuration){
        List<String> allTypePaths = new ArrayList<>();
        try{
            JsonNode configNode = OBJECT_MAPPER.readTree(cd_configuration);
            if (configNode.isArray()) {
                for (JsonNode currConfig : configNode) {
                    JsonNode deployPackages = currConfig.get("deployPackageFolder");
                    if (deployPackages != null) {
                        for (JsonNode currDeployPackage : deployPackages) {
                            // DP_110_ecp_cms-vts-common-svc, DP_100_manual_updateSecret
                            currDeployPackage.fieldNames().forEachRemaining(allTypePaths::add);
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return allTypePaths;
    }

    private static List<String> retrieveTypePathsFromJFrogResp(String jFrogResp){
        List<String> allTypePaths = new ArrayList<>();
        try {
            JsonNode results = OBJECT_MAPPER.readTree(jFrogResp).get("results");
            for (JsonNode currResult: results){
                String path = currResult.get("path").asText();
                // path e.g. CMS/OPMOE/CMS_MOE_CMSAF_APP_JDK8/M-ITOCMS-24-1232/DP_40_corp-db_UpdateForwarder/DB_SERVER_LIST_CORP/corp
                String[] pathParts = path.split("/");
                int keyIndex = getTypeIndexFromJFrogPathParts(pathParts);
                if (keyIndex >= 0 && keyIndex < pathParts.length){ // && pathParts[keyIndex].contains("DP") -> not necessary due to revised API payload
                    allTypePaths.add(pathParts[keyIndex]);
                }// else allTypePaths.add("N/A");  // actually, if the program is working fine, shouldn't have N/A at all
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allTypePaths;
    }

    private static int getTypeIndexFromJFrogPathParts(String[] pathParts){
        for (int i = 0; i < pathParts.length; i++){
            if(pathParts[i].contains("ITOCMS")) return i + 1;
        }
        return -1;
    }

    public static void parseJiraTicketInfoFromITOCMSKeyResp(String jiraResp, String key_ITOCMS, boolean isBiweekly){
        String affectedHosp = "";
        String k2FormLink = "";
        try{
            JsonNode issues = OBJECT_MAPPER.readTree(jiraResp).get("issues");
            PromoForm promoForm = DATA_CENTER.getPromoFormByKey_ITOCMS(key_ITOCMS);
            Map<String, Set<String>> allEndingTicketRelationshipMap = new LinkedHashMap<>();
            for(JsonNode issue: issues){
                JsonNode fields = issue.get("fields");
                if(affectedHosp.isBlank()){
                    affectedHosp = retrieveAffectedHosp(fields);
                    promoForm.affectedHosp(affectedHosp);
                }
                if(k2FormLink.isBlank()){
                    String srcK2FormLink = fields.get("customfield_11400").asText();
                    if (!srcK2FormLink.equalsIgnoreCase("null")) {
                        k2FormLink = srcK2FormLink;
                        String k2FormNo = retrieveK2FormNoFromLink(k2FormLink);
                        promoForm.k2FormLink(k2FormLink).k2FormNo(k2FormNo);
                    }
                }
                if(isBiweekly){
                    JsonNode issueLinks = fields.get("issuelinks");
                    allEndingTicketRelationshipMap.putAll(processIssueLinks(issueLinks));
                }
            }
            if (isBiweekly) {
                promoForm.endingTicketRelationshipMap(allEndingTicketRelationshipMap);
                Map<String, String> tempEndingTicketSummaryMap = new HashMap<>();
                for(String endingTicket: allEndingTicketRelationshipMap.keySet()){
                    String endingTicketSummary = APIQueryService.fetchTicketSummary(endingTicket);;
                    tempEndingTicketSummaryMap.put(endingTicket, endingTicketSummary);
                }
                promoForm.endingTicketSummaryMap(tempEndingTicketSummaryMap);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static Map<String, Set<String>> processIssueLinks(JsonNode issueLinks){
        Map<String, Set<String>> endingTicketRelationshipMap = new LinkedHashMap<>();
        for(JsonNode link: issueLinks){
            String endingTicket = "", relationship = "";
            try{
                if (link.has("outwardIssue")) {
                    endingTicket = link.get("outwardIssue").get("key").asText();
                    relationship = link.get("type").get("outward").asText();
                } else {
                    endingTicket = link.get("inwardIssue").get("key").asText();
                    relationship = link.get("type").get("inward").asText();
                }
                if (relationship.contains("has to")){
                    // e.g. has to be done before -> done before
                    String abridgedRelationship = relationship.replaceAll("has to be", "").strip();
                    if(endingTicketRelationshipMap.containsKey(endingTicket)){
                        endingTicketRelationshipMap.get(endingTicket).add(abridgedRelationship);
                    }else{
                        Set<String> relationshipSet = new LinkedHashSet<>();
                        relationshipSet.add(abridgedRelationship);
                        endingTicketRelationshipMap.put(endingTicket, relationshipSet);
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return endingTicketRelationshipMap;
        // after getting the results, should check if the map is empty
        // empty means no relationship at all
    }

    private static String retrieveK2FormNoFromLink(String k2FormLink){
        try{
            return k2FormLink.split("=")[1];
        }catch (Exception e){
            e.printStackTrace();
            return "N/A";
        }
    }

    private static String retrieveK2FormLink(String rawLink){
        try{
            return rawLink.substring("Promotion Form: ".length(), rawLink.indexOf("\n")).replaceAll("(\r|\n|\r\n)", "");
        }catch(StringIndexOutOfBoundsException e) {
            return rawLink.substring("Promotion Form: ".length()).replaceAll("(\r|\n|\r\n)", "");
            // likely the link does not have any \n at the end
        }
    }

    private static String retrieveAffectedHosp(JsonNode fields){
        String result = "";
        try{
            String rawAffectedHosp = fields.get("customfield_11887").asText();
            if(!rawAffectedHosp.equalsIgnoreCase("null")){
                String[] regexModified = rawAffectedHosp
                        .replaceAll(AFFECTED_HOSP_REGEX, "")
                        .split("(\r\n|\r|\n)");
                List<String> relevant = new ArrayList<>();
                for(String line: regexModified){
                    line = line.replaceAll("[\\s\\u00A0]+", " ").strip();
                    if(line.matches("\\W+") || line.isBlank()) continue;
                    relevant.add(line);
                }
                result =  String.join("\n", relevant);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    private static boolean isCurrentBatch(JsonNode fields){
        String biweeklyHint = fields.get("customfield_10519").asText();
        if(biweeklyHint.equalsIgnoreCase("null")) return true;
        String promoSchedule = "";
        try {
            promoSchedule = fields.get("customfield_10519").get("value").asText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // "value": "2024-13 ( PPM: 26-Sep-2024; AAT: 03-Oct-2024)
        return promoSchedule.contains(PROMO_RELEASE_EMAIL_CONFIG.getYear() + "-" + PROMO_RELEASE_EMAIL_CONFIG.getBatch());
    }

    public static String retrieveTicketSummary(String response) {
        // from the following jql: e.g. jql=cf[11599]~ENOTI-380&fields=summary
        String result = "";
        try {
            JsonNode issues = OBJECT_MAPPER.readTree(response).get("issues");
            if (issues.isArray()){
                for (JsonNode currIssue: issues){
                    String summary = currIssue.get("fields").get("summary").asText();
                    if (!summary.equalsIgnoreCase("null") && !summary.contains("\\s+")) {
                        return summary;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
