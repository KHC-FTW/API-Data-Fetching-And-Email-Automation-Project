package com.crc2jasper.jiraK2DataFetching;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailScheduler {

    private static final SingletonConfig singletonConfig = SingletonConfig.getInstance();
    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();
    private static final AllFormData allFormData = AllFormData.getInstance();


//    @Scheduled(cron = "*/30 * * * * *")

    // Start at 17:00 every day
    @Scheduled(cron = "0 0 17 * * *")
    public static void sendUrgentServiceEmail(){
        allFormData.clearAllEmailFormData();
        APIQueryService.fetchJiraUrgentServiceAPI();
        // Exception may be due to parameter passing, try not to pass parameter
        // all methods can't have parameters, must do it in the method itself
        EmailService.sendUrgentServiceEmail();
        allFormData.clearAllEmailFormData();
    }

    // Check at 17:30 every day
    @Scheduled(cron = "0 30 17 * * *")
    public static void sendBiweeklyEmail(){
        if(EmailService.dailyCheckForNewRelease()){
            String rawJiraAPI = singletonConfig.getRawJiraAPIBiweeklyPrn();
            String year = promotionRelease.getYear();   //2024
            String batch = promotionRelease.getBatch(); //13
            String year_batch = year + "_" + batch; //2024-13
            String polishedJiraAPI = String.format(rawJiraAPI, year_batch, year);
            // https://hatool.home/jira/rest/api/2/search?jql=
            // + project = ITOCMS AND summary ~ "PPM%s*" OR (summary ~ "PRN%s*" AND created >= -60d) AND "Promotion Schedule" is not EMPTY ORDER BY summary
            // &maxResults=1000&fields=customfield_11400&fields=summary&fields=description&fields=customfield_11628&fields=status&fields=customfield_10519&fields=customfield_14500

            allFormData.clearAllEmailFormData();
            APIQueryService.fetchJiraBiweeklyAPI();
            EmailService.sendBiweeklyEmail();
            promotionRelease.setToResendTmr();
            allFormData.clearAllEmailFormData();
        }
    }

    // Check at 09:00 every day
    @Scheduled(cron = "0 0 9 * * *")
    public static void resendBiweeklyEmail(){
        if(promotionRelease.getResendTmrStatus()){
            String rawJiraAPI = singletonConfig.getRawJiraAPIBiweeklyPrn();
            String year = promotionRelease.getYear();   //2024
            String batch = promotionRelease.getBatch(); //13
            String year_batch = year + "_" + batch; //2024-13
            String polishedJiraAPI = String.format(rawJiraAPI, year_batch, year);
            // https://hatool.home/jira/rest/api/2/search?jql=
            // + project = ITOCMS AND summary ~ "PPM%s*" OR (summary ~ "PRN%s*" AND created >= -60d) AND "Promotion Schedule" is not EMPTY ORDER BY summary
            // &maxResults=1000&fields=customfield_11400&fields=summary&fields=description&fields=customfield_11628&fields=status&fields=customfield_10519&fields=customfield_14500

            allFormData.clearAllEmailFormData();
            APIQueryService.fetchJiraBiweeklyAPI();
            EmailService.sendBiweeklyEmail();
            promotionRelease.resetResendTmrStatus();
            allFormData.clearAllEmailFormData();
        }
    }

    @Deprecated
    public static void simulateSendBiweeklyEmail(){
        if(EmailService.dailyCheckNewReleaseSimulation()){
            String rawJiraAPI = singletonConfig.getRawJiraAPIBiweeklyPrn();
            String year = promotionRelease.getYear();   //2024
            String batch = promotionRelease.getBatch(); //13
            String year_batch = year + "_" + batch; //2024-13
            String polishedJiraAPI = String.format(rawJiraAPI, year_batch, year);
            // https://hatool.home/jira/rest/api/2/search?jql=
            // + project = ITOCMS AND summary ~ "PPM%s*" OR (summary ~ "PRN%s*" AND created >= -60d) AND "Promotion Schedule" is not EMPTY ORDER BY summary
            // &maxResults=1000&fields=customfield_11400&fields=summary&fields=description&fields=customfield_11628&fields=status&fields=customfield_10519&fields=customfield_14500

            allFormData.clearAllEmailFormData();
            APIQueryService.fetchJiraBiweeklyAPI();
            EmailService.sendBiweeklyEmail();
            promotionRelease.setToResendTmr();
            allFormData.clearAllEmailFormData();
        }
    }
}
