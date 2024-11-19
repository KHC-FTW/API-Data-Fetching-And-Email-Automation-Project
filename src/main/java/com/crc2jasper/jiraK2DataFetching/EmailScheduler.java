package com.crc2jasper.jiraK2DataFetching;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailScheduler {

    private static final SingletonConfig singletonConfig = SingletonConfig.getInstance();
    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();
    private static final AllFormData allFormData = AllFormData.getInstance();


//    @Scheduled(cron = "*/30 * * * * *")


    @Scheduled(cron = "0 0 17 * * *")
    public static void sendUrgentServiceEmail(){
        allFormData.clearAllEmailFormData();
        SystemIni.startAPICall(singletonConfig.getFullJiraAPIUrgentService());
        EmailService.sendEmail(singletonConfig.getEmailSubjectUrgentService());
        allFormData.clearAllEmailFormData();
    }


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
            SystemIni.startAPICall(polishedJiraAPI);
            EmailService.sendEmail(singletonConfig.getEmailSubjectBiweekly());
            promotionRelease.setToResendTmr();
            allFormData.clearAllEmailFormData();
        }
    }

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
            SystemIni.startAPICall(polishedJiraAPI);
            EmailService.sendEmail(singletonConfig.getEmailSubjectBiweekly());
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
            SystemIni.startAPICall(polishedJiraAPI);
            EmailService.sendEmail(singletonConfig.getEmailSubjectBiweekly());
            promotionRelease.setToResendTmr();
            allFormData.clearAllEmailFormData();
        }
    }
}
