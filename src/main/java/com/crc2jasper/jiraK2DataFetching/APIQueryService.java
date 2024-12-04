package com.crc2jasper.jiraK2DataFetching;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;


public class APIQueryService {

    private APIQueryService(){}

    private static final WebClient webClient = WebClientConfig.customWebClient();
    private static final String username = SingletonConfig.getInstance().getAdminUsername();
    private static final String password = SingletonConfig.getInstance().getAdminPassword();
    private static final String jFrogAPI = SingletonConfig.getInstance().getJfrogAPI();
    private static final SingletonConfig singletonConfig = SingletonConfig.getInstance();
    private static final String basicAuth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

    public static void fetchJiraUrgentServiceAPI() {
        String response = webClient
                .get()
                .uri(singletonConfig.getFullJiraAPIUrgentService())
                .headers(headers -> headers.setBasicAuth(username, password))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        DataManip.jiraRespJsonManip(response, false);
    }

    public static void fetchJiraBiweeklyAPI() {
        String response = webClient
                .get()
                .uri(singletonConfig.getFullJiraAPIBiweeklyPrn())
                .headers(headers -> headers.setBasicAuth(username, password))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        DataManip.jiraRespJsonManip(response, true);
    }

    public static String fetchJiraAffectedHospAPI(String key){
        String targetAPI = singletonConfig.getJiraRestAPI() + String.format("cf[10508]~%s&fields=customfield_11887", key);
        String response = webClient
                .get()
                .uri(targetAPI)
                .headers(headers -> headers.setBasicAuth(username, password))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return DataManip.jiraAffectedHospRespManip(response);
    }

    public static String fetchCrLinkedSummary(String endingCrTicket){
        // cf[11599]~NDORS-705&fields=summary
        String targetAPI = singletonConfig.getJiraRestAPI() + String.format("cf[11599]~%s&fields=summary", endingCrTicket);
        String response = webClient
                .get()
                .uri(targetAPI)
                .headers(headers -> headers.setBasicAuth(username, password))
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

        String response = webClient
                .post()
                .uri(jFrogAPI)
                .headers(headers -> headers.setBasicAuth(username, password))
                .header("Content-Type", "text/plain")
                .bodyValue(json)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return DataManip.jFrogRespJsonManip(formSummary, response);
    }

    public static List<String> collabNetInitialAPI(String appendingItem){
        // appendingItem is the related Jira ticket no. e.g. PROMIS-46
        final String targetAPI = "http://cdrasvn:90/svn/cicd/trunk/releasesource/CMS/" + appendingItem + "/";
        // e.g. http://cdrasvn:90/svn/cicd/trunk/releasesource/CMS/PROMIS-46/
        List<String> allTargetPackagePaths = new ArrayList<>();
        try{
            Connection connection = Jsoup.connect(targetAPI).header("Authorization", "Basic " + basicAuth);
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
            Connection connection = Jsoup.connect(targetAPI).header("Authorization", "Basic " + basicAuth);
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

}
