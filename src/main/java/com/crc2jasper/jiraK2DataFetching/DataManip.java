package com.crc2jasper.jiraK2DataFetching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Pattern;


public class DataManip {
    private DataManip(){}
    private static final ObjectMapper objectMapper = SystemIni.getObjectMapper();
    private static final AllFormData allformData = AllFormData.getInstance();
    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();
    private static final TextSummary textSummary = TextSummary.getInstance();
    private static final String AFFECTED_HOSP_REGEX = "\\*|Affected Hospital / Effective Date:|\\{color:.{0,8}}|\\{color\\}|\\\\u[0-9A-Fa-f]{4}\"|<[^>]*>|&[a-zA-Z0-9#]+;";
//    private static final Pattern AFFECTED_HOSP_PATTERN = Pattern.compile(AFFECTED_HOSP_REGEX);

    public static String getK2FormLink(String originalLink){
//        int length = originalLink.length();
        try{
            return originalLink.substring("Promotion Form: ".length(), originalLink.indexOf("\n"));
        }catch(StringIndexOutOfBoundsException e) {
            return originalLink.substring("Promotion Form: ".length());
            // likely the link does not have any \n at the end
        }
    }

    public static String getK2FormNo(String formLink){
        String[] parts = formLink.split("=");
        return parts[1];
        //                                                                            v
        // e.g. https://wfeng-svc/Runtime/Runtime/Form/CMS__Promotion__Form?formnumber=M-ITOCMS-24-1179
        // -> [https://wfeng-svc/Runtime/Runtime/Form/CMS__Promotion__Form?formnumber, M-ITOCMS-24-1179]
    }

    public static void jiraRespJsonManip(String jiraResp, boolean genReadMe){
        JsonNode jsonArray = null;
        String readmePromotion;
        String readmeAllTypes;
        String readmeTargetHosp;
        String readmeCrInfo;
        String readmeStatus;

        try {
            jsonArray = objectMapper.readTree(jiraResp);
            JsonNode issue = jsonArray.get("issues");
            if (issue != null && issue.isArray()){
                for (JsonNode currIssue: issue){
                    String biweeklyHint = currIssue.get("fields").get("customfield_10519").toString();
                    if (!biweeklyHint.equals("null")){ // if null, should be urgent/service release
                        // if not null, is biweekly, but we have to check if it belongs to the current batch
                        String promoSchedule = objectMapper.treeToValue(currIssue.get("fields").get("customfield_10519").get("value"), String.class);
                        // "value": "2024-13 ( PPM: 26-Sep-2024; AAT: 03-Oct-2024)
                        String year = promotionRelease.getYear();   //2024
                        String batch = promotionRelease.getBatch(); //13
                        String year_batch = year + "-" + batch; //2024-13
                        if(!promoSchedule.contains(year_batch)) continue;   //not the current batch, can skip the rest
                    }

                    EmailForm emailForm = new EmailForm();

                    //Target Date
                    String targetDate = objectMapper.treeToValue(currIssue.get("fields").get("customfield_11628"), String.class);   //e.g. 18-Nov-2024
                    boolean isToday = false;
                    if(!targetDate.isEmpty()){  // likely URGENT/SERVICE release
                        isToday = TimeUtil.checkIsToday(targetDate);
                        targetDate = TimeUtil.dateDayOfWeekFormatter(targetDate);
                    }else{  //likely batch release, format the date to be that batch e.g. 2024-13
                        String year = promotionRelease.getYear();
                        String batch = promotionRelease.getBatch();
                        String year_batch = year + "-" + batch;
                        targetDate = year_batch;
                    }
                    // Jira Key e.g. ITOCMS-35743
                    String formKey = objectMapper.treeToValue(currIssue.get("key"), String.class);

                    // TODO: Get target hospitals from CR ticket, extracted from description
                    // String relatedCRTicket = extractRelatedCRTicketFromDesc(formDescription);
                    // String jql = String.format("key = %s", relatedCRTicket);
                    // customfield_11887

                    // cf[10508]~%s&fields=customfield_11887 (%s is the jira key i.e. formKey)

                    String affectedHosp = APIQueryService.fetchJiraAffectedHospAPI(formKey);
                    if (affectedHosp.isBlank()) affectedHosp = "N/A";

                    // Description e.g. GCRS-564: Update worklist type for DH-PHLC specimen type
                    String formDescription = objectMapper.treeToValue(currIssue.get("fields").get("description"), String.class);

                    // e.g. Promotion Form: https://wfeng-svc/Runtime/Runtime/Form/CMS__Promotion__Form?formnumber=M-ITOCMS-24-1179
                    String promoForm = objectMapper.treeToValue(currIssue.get("fields").get("customfield_11400"), String.class);
                    String formLink = "", formNo = "";
                    if (promoForm != null){
                        formLink = getK2FormLink(promoForm);
                        formNo = getK2FormNo(formLink);
                    }else{
                        formNo = "N/A";
                    }

                    // Status e.g. Production, QC, Pre-Production Verification, Validation, Acceptance, etc.
                    String status = objectMapper.treeToValue(currIssue.get("fields").get("status").get("name"), String.class);

                    // Summary e.g. PPM2024_S0306, PRN2024_RG0089
                    String formSummary = objectMapper.treeToValue(currIssue.get("fields").get("summary"), String.class);

                    emailForm.targetDate(targetDate)
                            .affectedHosp(affectedHosp)
                            .key(formKey)
                            .summary(formSummary)
                            .description(formDescription)
                            .promotionFormLink(formLink)
                            .promotionFormNo(formNo)
                            .status(status)
                            .isToday(isToday);

                    // Add to all form data first for getting later and assume it is related until we know its type
                    // Type info is done in the following part
                    allformData.addRelatedEmailForm(formSummary, emailForm);

                    if (formSummary.contains("PRN")){      // PRN promotion: ad hoc type; 99.9% of the time it's unrelated to imp
                        // special case of PRN promotion
                        // 2 ways to check type:
                        // 1) By customfield_14500
                        // 2) By http://cdrasvn:90/

                        // customfield_14500 => cd_configuration
                        String cd_configuration = objectMapper.treeToValue(currIssue.get("fields").get("customfield_14500"), String.class);
                        if (cd_configuration != null){
                            // convert to Json String first and back to Json Node for easier manipulation
                            JsonNode configNode = objectMapper.readTree(cd_configuration);
                            if (configNode.isArray()){
                                List<String> allTypePaths = new ArrayList<>();
                                for (JsonNode currConfig: configNode){
                                    JsonNode deployPackages = currConfig.get("deployPackageFolder");
                                    if (deployPackages != null){
                                        for (JsonNode currDeployPackage: deployPackages){
                                            // DP_110_ecp_cms-vts-common-svc, DP_100_manual_updateSecret
                                            currDeployPackage.fieldNames().forEachRemaining(allTypePaths::add);
                                        }
                                    }
                                }
                                processTypesFromPaths(allTypePaths, formSummary);
                            }

                        }else{
                            // cannot get PRN types from Jira, need to get from http://cdrasvn:90/
                            String relatedCRTicket = extractRelatedCRTicketFromDesc(formDescription);
                            List<String> allTypePaths = APIQueryService.collabNetInitialAPI(relatedCRTicket);
                            processTypesFromPaths(allTypePaths, formSummary);
                        }
                    }else{
                        // Belong to PPM Type, will fetch from JFrog
                        List<String> allTypePaths = APIQueryService.fetchJfrogAPI(formNo, formSummary);
                        processTypesFromPaths(allTypePaths, formSummary);

                        if(genReadMe){
                            Map<String, String> allCRResults = TextSummary.getAllCRTicketsFromDesc(formSummary, formDescription);
                            String crTickets = allCRResults.get("crTickets");
                            String crInfo = allCRResults.get("crInfo");
                            readmePromotion = TextSummary.abridgedSummary(formSummary) + "_" + crTickets;
                            readmeAllTypes = TextSummary.typeMapToSetString(emailForm.getTypes());
                            readmeTargetHosp = affectedHosp;
                            readmeCrInfo = crInfo;
                            readmeStatus = status;

                            ReadmeItem readmeItem = new ReadmeItem(readmePromotion, readmeAllTypes, readmeTargetHosp, readmeCrInfo, readmeStatus);
                            textSummary.addReadmeItem(readmeItem);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage() + "\n");
        }
    }

    public static String jiraCRLinkedSummaryManip(String jiraResp){
        // from the following jql: e.g. jql=cf[11599]~ENOTI-380&fields=summary
        String result = "";
        try {
            JsonNode jsonArray = objectMapper.readTree(jiraResp);
            JsonNode issues = jsonArray.get("issues");
            if (issues != null && issues.isArray()){
                for (JsonNode currIssue: issues){
                    String summary = objectMapper.treeToValue(currIssue.get("fields").get("summary"), String.class);
                    if (summary != null) {
                        return summary;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception raised when fetching Jira CR linked summary: " + e.getMessage() + "\n");
            return result;
        }
        return result;
    }

    public static String jiraAffectedHospRespManip(String jiraResp){
        String result = "";
        try {
            JsonNode jsonArray = objectMapper.readTree(jiraResp);
            JsonNode issues = jsonArray.get("issues");
            if (issues != null && issues.isArray()){
                for (JsonNode currIssue: issues){
                    String affectedHosp = objectMapper.treeToValue(currIssue.get("fields").get("customfield_11887"), String.class);
                    if (affectedHosp != null){
                        if(!affectedHosp.isBlank()){
                            String[] regexModified = affectedHosp.replaceAll(AFFECTED_HOSP_REGEX, "")
                                    .replaceAll("[{}]", "")
                                    .split("(\r\n|\r|\n)");
                            List<String> relevant = new ArrayList<>();
                            for(String line: regexModified){
                                line = line.replaceAll("[\\s|\\u00A0]+", " ").strip();
                                if(!line.isBlank()){
                                    relevant.add(line);
                                }
                            }
                            return String.join("\n", relevant);
                        }

//                                .replaceAll("(\r\n|\r|\n)+", "\n")
//                                .replaceAll("(\\{|}|^\\s*$)", "")
//                                .replaceAll("[\\s+|\\u00A0]", " ")
//                                .replaceAll("[<>]", "*")
////                                .replaceAll("(?<=\\p{Alpha})\\*", "*\n")
//                                .strip();
                    }
                }
            }
        } catch (JsonProcessingException e) {
            System.out.println("Exception raised when fetching Jira affected hospitals: " + e.getMessage() + "\n");
        }
        return result;
    }

    public static List<String> jFrogRespJsonManip(String formSummary, String jFrogResp){
        JsonNode jsonArray = null;
        List<String> allTypePaths = new ArrayList<>();
        try {
            jsonArray = objectMapper.readTree(jFrogResp);
            JsonNode results = jsonArray.get("results");
            if (results != null && results.isArray()){
                boolean hasAnyImpHospOrCorp = false;
                for (JsonNode currResult: results){
                    String path = objectMapper.treeToValue(currResult.get("path"), String.class);
                    // path e.g. CMS/SWR/CMS_SWR_PROGRESS_NOTE_SVC_JDK8/M-ITOCMS-24-1065/FB_020_corp-db_InsertCorpForwarder/DB_SERVER_LIST_CORP/corp
                    String[] pathParts = path.split("/");
                    int pathSize = pathParts.length;
                    int keyIndex = getTypeIndexFromJFrogPathParts(pathParts);
                    if (keyIndex >= 0 && keyIndex < pathSize && pathParts[keyIndex].contains("DP")){
                        allTypePaths.add(pathParts[keyIndex]);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage() + "\n");
        }
        return allTypePaths;
    }

    private static void processTypesFromPaths(List<String> paths, String formSummary){
        // path must be of this format
        // e.g. DP_020_corp-db_InsertCorpForwarder      (jFrog)
        // e.g. DP_110_ecp_cms-vts-common-svc           (jira cd_configuration)
        // e.g. DP_101_manual_createVaultValues         (CollabNet)

        // formSummary as the identifier for the email form
        boolean hasAnyImpHospOrCorp = false;
        for (String path: paths){
            // String[] type:
            // index 0: sequence no. (still as string, not parsed to int yet)
            // index 1: actual type e.g. imp-hosp-db, imp-corp-db
            String[] type = extractTypeFromPath(path);
            if (!hasAnyImpHospOrCorp) hasAnyImpHospOrCorp = isImpHospOrImpCorp(type[1]);
            allformData.getRelatedEmailForm(formSummary).addType(Integer.parseInt(type[0]), type[1]);
        }
        if(!hasAnyImpHospOrCorp) {
            // after checking all paths, can't find any imp-hosp or imp-corp
            EmailForm unrelatedEmailForm = allformData.getRelatedEmailForm(formSummary);
            allformData.removeRelatedEmailForm(formSummary);
            allformData.addUnrelatedEmailForm(formSummary, unrelatedEmailForm);
        }
    }

    private static String extractRelatedCRTicketFromDesc(String formDescription){
        // get related CR ticket from description
        // -> this is the target information for us to call from http://cdrasvn:90/
        int targetIdx = formDescription.indexOf(":");
        if (targetIdx != -1) {  // ":" found, which is the usual case
            return formDescription.substring(0, targetIdx);
        }else { // in case no ":" found, need another way to extract the related CR ticket
            boolean foundDigit = false;
            for (int i = 0; i < formDescription.length(); i++){
                if (Character.isDigit(formDescription.charAt(i))) {
                    foundDigit = true;
                }
                if(foundDigit && !Character.isDigit(formDescription.charAt(i))){
                    targetIdx = i;
                    break;
                }
            }
            return formDescription.substring(0, targetIdx);
        }
    }

    private static int getTypeIndexFromJFrogPathParts(String[] pathParts){
        int pathSize = pathParts.length;
        for (int i = 0; i < pathSize; i++){
            if(pathParts[i].contains("ITOCMS")){
                return i + 1;
            }
        }
        return -1;
    }

    private static boolean isImpHospOrImpCorp(String type){
        return type.equalsIgnoreCase("imp-hosp-db") || type.equalsIgnoreCase("imp-corp-db");
    }

    private static String[] extractTypeFromPath(String pathPart){
        String[] parts = pathPart.split("_");
        return new String[]{parts[1], parts[2]};
    }

    @Deprecated
    private static String getPPMTypeFromSummary(String summary){
        return summary.contains("U") ? "Urgent" : "Service";
    }

    @Deprecated
    public static String beautifyJsonString(String input){
        return input.replaceAll("[\",]", "").replaceAll("\\\\r", "");
    }
}
