package com.crc2jasper.jiraK2DataFetching;

import org.checkerframework.checker.units.qual.C;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailScheduler {

    private static final SingletonConfig singletonConfig = SingletonConfig.getInstance();
    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();
    private static final AllFormData allFormData = AllFormData.getInstance();
    private static final TextSummary textSummary = TextSummary.getInstance();
    private static final CRInfo crInfo = CRInfo.getInstance();


//    @Scheduled(cron = "*/10 * * * * *")

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
            eventSequenceBiweekly();
            // Set the flag to resend the email tomorrow
            promotionRelease.setToResendTmr();
        }
    }

    // Check at 09:00 every day
    @Scheduled(cron = "0 0 9 * * *")
    public static void resendBiweeklyEmail(){
        if(promotionRelease.getResendTmrStatus()){
            eventSequenceBiweekly();
            // Disable resend
            promotionRelease.resetResendTmrStatus();
        }
    }

//    @Deprecated
//    @Scheduled(cron = "*/10 * * * * *")
    public static void simulateSendBiweeklyEmail(){
        if(EmailService.dailyCheckNewReleaseSimulation()){
            eventSequenceBiweekly();
            // Set the flag to resend the email tomorrow
            promotionRelease.setToResendTmr();
        }
    }

    public static void manualTestSendBiweeklyEmail(String lastReleaseDate, String year, String batch){
        EmailService.manualTestBiweeklyRelease(lastReleaseDate, year, batch);
        eventSequenceBiweekly();
    }

    private static void eventSequenceBiweekly(){
        // Make sure we start anew
        allFormData.clearAllEmailFormData();
        // Start API call (biweekly normal promotion release)
        APIQueryService.fetchJiraBiweeklyAPI();
        // Start API call (urgent service for biweekly)
        APIQueryService.fetchJiraUrgentServiceForBiweeklyAPI();
        // Generate Readme content and save it to a local temp location
        TextSummary.writeReadMeTxt(textSummary.genReadMeContent());
        // Generate URL files for K2 forms
        UrlService.genUrlFile();
        // Compress all files to a zip file
        ZipService.compressFileToZip();
        // Send email with zip attached
        EmailService.sendBiweeklyEmail();
        // Clear all saved data first to avoid unwanted data are left behind
        allFormData.clearAllEmailFormData();
        textSummary.clearAllReadmeItems();
        crInfo.clearSummaryToLinkedIssues();
        // Delete all temporary directories and all their files
        DirectoryService.delDir();
    }

}
