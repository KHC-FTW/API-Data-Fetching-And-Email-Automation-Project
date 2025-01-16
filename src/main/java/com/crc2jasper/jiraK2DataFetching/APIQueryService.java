package com.crc2jasper.jiraK2DataFetching;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class APIQueryService {

    private APIQueryService(){}

    private static final WebClient CUSTOM_WEB_CLIENT = WebClientConfig.customWebClient();
    private static final String USERNAME = SingletonConfig.getInstance().getAdminUsername();
    private static final String PASSWORD = SingletonConfig.getInstance().getAdminPassword();
    private static final String JFROG_API = SingletonConfig.getInstance().getJfrogAPI();
    private static final SingletonConfig SINGLETON_CONFIG = SingletonConfig.getInstance();
    private static final String BASIC_AUTH = Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());

    public static void fetchJiraUrgentServiceAPI() {
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(SINGLETON_CONFIG.getFullJiraAPIUrgentService())
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        DataManip.jiraRespJsonManip(response, false);
    }

    public static void fetchJiraBiweeklyAPI() {
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(SINGLETON_CONFIG.getFullJiraAPIBiweeklyPrn())
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        DataManip.jiraRespJsonManip(response, true);
    }

    public static void fetchJiraUrgentServiceForBiweeklyAPI() {
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(SINGLETON_CONFIG.getFullJiraAPIUrgentServiceForBiweekly())
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        DataManip.jiraUrgentServiceForBiweeklyRespManip(response);
    }

    public static String fetchJiraAffectedHospAPI(String key){
        String targetAPI = SINGLETON_CONFIG.getJiraRestAPI() + String.format("cf[10508]~%s&fields=customfield_11887", key);
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(targetAPI)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return DataManip.jiraAffectedHospRespManip(response);
    }

    public static List<String> fetchJiraCrTicketLinkedIssues(String crTicket){
        String TARGET_API = SINGLETON_CONFIG.getJiraRestAPI() + String.format("key=%s&fields=issuelinks", crTicket);
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(TARGET_API)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return DataManip.jiraCrTicketLinkedIssuesRespManip(response);
    }

    public static String fetchCrLinkedSummary(String endingCrTicket){
        // cf[11599]~NDORS-705&fields=summary
        String targetAPI = SINGLETON_CONFIG.getJiraRestAPI() + String.format("cf[11599]~%s&fields=summary", endingCrTicket);
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(targetAPI)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return DataManip.jiraCRLinkedSummaryManip(response);
    }

    public static List<String> fetchJfrogAPI(String k2FormNo, String formSummary){
        String json = String.format("""
                      items.find(
                        {
                            "repo": {"$eq": "cms_cicd_package"},
                            "path": {"$match": "*%s*"},
                            "path": {"$match": "*DP_*"}
                        }
                      )
                      """, k2FormNo);
        // revised payload to include "path": {"$match": "*DP_*"} -> more specific json data can be returned

        String response = CUSTOM_WEB_CLIENT
                .post()
                .uri(JFROG_API)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .header("Content-Type", "text/plain")
                .bodyValue(json)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return DataManip.jFrogRespJsonManip(formSummary, response);
    }

    public static List<String> collabNetInitialAPI(String parentTicket){
        // appendingItem is the related Jira ticket no. e.g. PROMIS-46
        final String targetAPI = "http://cdrasvn:90/svn/cicd/trunk/releasesource/CMS/" + parentTicket + "/";
        // e.g. http://cdrasvn:90/svn/cicd/trunk/releasesource/CMS/PROMIS-46/
        List<String> allTargetPackagePaths = new ArrayList<>();
        try{
            Connection connection = Jsoup.connect(targetAPI).header("Authorization", "Basic " + BASIC_AUTH);
            Document doc = connection.get();
            doc.getElementsByTag("li").forEach(content -> {
                List<String> tempResults = collabNewFinalAPI(targetAPI, content.text());
                if (!tempResults.isEmpty()) allTargetPackagePaths.addAll(tempResults);
            });
        }catch (Exception e){
            System.out.println("Error in collabNetInitialAPI: " + e.getMessage() + ".\n");
        }
        return allTargetPackagePaths;
    }

    private static List<String> collabNewFinalAPI(String apiPath, String appendingItem){
        // appendingItem is something like cms-promis-main-svc-ocp4, based on the directory from related Jira ticket no
        final String targetAPI = apiPath + appendingItem + "structurePackage/";
        // full target path e.g. http://cdrasvn:90/svn/cicd/trunk/releasesource/CMS/PROMIS-46/cms-promis-main-svc-ocp4/structurePackage/
        List<String> targetPackagePaths = new ArrayList<>();
        if (appendingItem.equals("..")) return targetPackagePaths;
        try{
            Connection connection = Jsoup.connect(targetAPI).header("Authorization", "Basic " + BASIC_AUTH);
            Document doc = connection.get();
            doc.getElementsByTag("li").forEach(item -> {
                String content = item.text();
                if (!content.equals("..") && content.contains("DP")){
                    targetPackagePaths.add(content);
                }
            });
        }catch (Exception e){
            System.out.println("Error in collabNewFinalAPI: " + e.getMessage() + ".\n");
        }
        return targetPackagePaths;
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void fetchJiraUrgentServiceAPI_V2() {
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(SINGLETON_CONFIG.getFullJiraAPIUrgentService())
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonDataParser.parseStandardJiraResp(response, false);
    }

    public static void fetchJiraBiweeklyAPI_V2() {
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(SINGLETON_CONFIG.getFullJiraAPIBiweeklyPrn())
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonDataParser.parseStandardJiraResp(response, true);
    }

    public static void fetchJiraUrgentServiceForBiweeklyAPI_V2() {
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(SINGLETON_CONFIG.getFullJiraAPIUrgentServiceForBiweekly())
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonDataParser.parseJiraUrgentServiceForBiweeklyResp(response);
    }

    public static void jiraTicketInfoFromITOCMSKey(String key_ITOCMS, boolean isBiweekly){
        String targetAPI = SINGLETON_CONFIG.getJiraRestAPI() + String.format("cf[10508]~%s%s", key_ITOCMS, SINGLETON_CONFIG.getJiraFields());
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(targetAPI)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonDataParser.parseJiraTicketInfoFromITOCMSKeyResp(response, key_ITOCMS, isBiweekly);
    }

    public static String fetchJfrogAPIForTypes(String k2FormNo){
        String json = String.format("""
                      items.find(
                        {
                            "repo": {"$eq": "cms_cicd_package"},
                            "path": {"$match": "*%s*"},
                            "path": {"$match": "*DP_*"}
                        }
                      )
                      """, k2FormNo);
        // revised payload to include "path": {"$match": "*DP_*"} -> more specific json data can be returned

        return CUSTOM_WEB_CLIENT
                .post()
                .uri(JFROG_API)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .header("Content-Type", "text/plain")
                .bodyValue(json)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

//        return DataManip.jFrogRespJsonManip(formSummary, response);
    }

    public static String fetchTicketSummary(String ticket){
        // cf[11599]~NDORS-705&fields=summary
        String targetAPI = SINGLETON_CONFIG.getJiraRestAPI() + String.format("cf[11599]~%s&fields=summary", ticket);
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(targetAPI)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return JsonDataParser.retrieveTicketSummary(response);
    }

    public static Map<String, String> fetchTicketSummaryAndRelatedTickets(String ticket){
        // cf[11599]~NDORS-705&fields=summary
        String targetAPI = SINGLETON_CONFIG.getJiraRestAPI() + String.format("cf[11599]~%s%s", ticket, SINGLETON_CONFIG.getJiraFields());
        String response = CUSTOM_WEB_CLIENT
                .get()
                .uri(targetAPI)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return JsonDataParser.retrieveTicketSummaryAndRelatedTickets(response);
//        return DataManip.jiraCRLinkedSummaryManip(response);
    }

    public static String fetchTicketIssueLinks(String tickets){
        // cf[11599]~NDORS-705&fields=summary
        String targetAPI = SINGLETON_CONFIG.getJiraRestAPI() + String.format("key in (%s) order by key asc %s", tickets, SINGLETON_CONFIG.getJiraFields());
        return CUSTOM_WEB_CLIENT
                .get()
                .uri(targetAPI)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
