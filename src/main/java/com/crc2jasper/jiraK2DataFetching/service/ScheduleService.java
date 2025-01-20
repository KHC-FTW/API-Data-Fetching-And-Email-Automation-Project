package com.crc2jasper.jiraK2DataFetching.service;

import com.crc2jasper.jiraK2DataFetching.component.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduleService {
    private ScheduleService(){}
    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = PromoReleaseEmailConfig.getInstance();
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();

    /////////////////////////// V2 ///////////////////////////
    // Start at 17:00 every day
    @Scheduled(cron = "0 0 17 * * *")
    public static void sendUrgentServiceEmail_V2(){
        DATA_CENTER.clearAllData();
        APIQueryService.fetchJiraUrgentServiceAPI_V2();
        EmailService.sendUrgentServiceEmail_V2();
        DATA_CENTER.clearAllData();
    }

    @Scheduled(cron = "0 30 17 * * *")
    public static void sendBiweeklyEmail_V2(){
        if(EmailService.checkDailyForNewBiweeklyRelease()){
            eventSequenceBiweekly_V2_multiThreaded();
            // Set the flag to resend the email tomorrow
            PROMO_RELEASE_EMAIL_CONFIG.setToResendTmr();
        }
    }

    // Check at 09:00 every day
    @Scheduled(cron = "0 0 9 * * *")
    public static void resendBiweeklyEmail_V2(){
        if(PROMO_RELEASE_EMAIL_CONFIG.getResendTmrStatus()){
            eventSequenceBiweekly_V2_multiThreaded();
            // Disable resend
            PROMO_RELEASE_EMAIL_CONFIG.resetResendTmrStatus();
        }
    }

//    @Scheduled(cron = "*/10 * * * * *")
    public static void simulateSendBiweeklyEmail(){
        if(EmailService.checkdailyForNewBiweeklyReleaseSimulation()){
            System.out.println("Executing ...");
            eventSequenceBiweekly_V2_multiThreaded();
            // Set the flag to resend the email tomorrow
            PROMO_RELEASE_EMAIL_CONFIG.setToResendTmr();
            System.out.println("Completed!");
        }
    }

    public static void manualTestSendBiweeklyEmail_V2_multiThreaded(String lastReleaseDate, String year, String batch){
        System.out.println("Executing ...");
        EmailService.manualTestBiweeklyRelease(lastReleaseDate, year, batch);
        eventSequenceBiweekly_V2_multiThreaded();
        System.out.println("Completed!");
    }

    public static void manualTestSendBiweeklyEmail_V2(String lastReleaseDate, String year, String batch){
        System.out.println("Executing ...");
        EmailService.manualTestBiweeklyRelease(lastReleaseDate, year, batch);
        eventSequenceBiweekly_V2();
        System.out.println("Completed!");
    }

    private static void eventSequenceBiweekly_V2_multiThreaded(){
        DATA_CENTER.clearAllData();
        Thread t1 = new Thread(APIQueryService::fetchJiraBiweeklyAPI_V2);
        Thread t2 = new Thread(APIQueryService::fetchJiraUrgentServiceForBiweeklyAPI_V2);
        t1.start(); t2.start();
        try {
            t1.join(); t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread t3 = new Thread(() -> ReadmeService.createReadmeFile(ReadmeService.genReadmeContent()));
        Thread t4 = new Thread(UrlService::genAllUrlFiles_V2);
        t3.start(); t4.start();
        try {
            t3.join(); t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ZipService.compressFileToZip();
        EmailService.sendBiweeklyEmailWithAttachment_V2();
        DATA_CENTER.clearAllData();
        DirectoryService.delDir();
    }

    private static void eventSequenceBiweekly_V2(){
        DATA_CENTER.clearAllData();
        APIQueryService.fetchJiraBiweeklyAPI_V2();
        APIQueryService.fetchJiraUrgentServiceForBiweeklyAPI_V2();
        ReadmeService.createReadmeFile(ReadmeService.genReadmeContent());
        UrlService.genAllUrlFiles_V2();
        ZipService.compressFileToZip();
        EmailService.sendBiweeklyEmailWithAttachment_V2();
        DATA_CENTER.clearAllData();
        DirectoryService.delDir();
    }

    //    @Scheduled(cron = "*/10 * * * * *")

    // Start at 17:00 every day
//    @Scheduled(cron = "0 0 17 * * *")
//    @Scheduled(cron = "*/10 * * * * *")
/*    public static void sendUrgentServiceEmail(){
        ALL_FORM_DATA.clearAllEmailFormData();
        APIQueryService.fetchJiraUrgentServiceAPI();
        // Exception may be due to parameter passing, try not to pass parameter
        // Current naive mitigation: all methods can't have parameters, must do it in the method itself
        EmailService.sendUrgentServiceEmail();
        ALL_FORM_DATA.clearAllEmailFormData();
    }*/

    // Check at 17:30 every day
/*    @Scheduled(cron = "0 30 17 * * *")
    public static void sendBiweeklyEmail(){
        if(EmailService.checkDailyForNewBiweeklyRelease()){
            eventSequenceBiweekly();
            // Set the flag to resend the email tomorrow
            PROMOTION_RELEASE.setToResendTmr();
        }
    }*/

    // Check at 09:00 every day
/*    @Scheduled(cron = "0 0 9 * * *")
    public static void resendBiweeklyEmail(){
        if(PROMOTION_RELEASE.getResendTmrStatus()){
            eventSequenceBiweekly();
            // Disable resend
            PROMOTION_RELEASE.resetResendTmrStatus();
        }
    }*/

//    @Deprecated
//    @Scheduled(cron = "*/10 * * * * *")
/*    public static void simulateSendBiweeklyEmail(){
        if(EmailService.dailyCheckNewReleaseSimulation()){
            eventSequenceBiweekly();
            // Set the flag to resend the email tomorrow
            PROMOTION_RELEASE.setToResendTmr();
        }
    }

    public static void manualTestSendBiweeklyEmail(String lastReleaseDate, String year, String batch){
        EmailService.manualTestBiweeklyRelease(lastReleaseDate, year, batch);
        eventSequenceBiweekly();
    }*/

    /*@Deprecated
    private static void eventSequenceBiweekly(){
        // Make sure we start anew
        ALL_FORM_DATA.clearAllEmailFormData();
        // Start API call (biweekly normal promotion release)
        APIQueryService.fetchJiraBiweeklyAPI();
        // Start API call (urgent service for biweekly)
        APIQueryService.fetchJiraUrgentServiceForBiweeklyAPI();
        // Generate Readme content and save it to a local temp location
        TextSummary.writeReadMeTxt(TEXT_SUMMARY.genReadMeContent());
        // Generate URL files for K2 forms
        UrlService.genUrlFile();
        // Compress all files to a zip file
        ZipService.compressFileToZip();
        // Send email with zip attached
        EmailService.sendBiweeklyEmail();
        // Clear all saved data first to avoid unwanted data are left behind
        ALL_FORM_DATA.clearAllEmailFormData();
        TEXT_SUMMARY.clearAllReadmeItems();
        CR_INFO.clearSummaryToLinkedIssues();
        // Delete all temporary directories and all their files
        DirectoryService.delDir();
    }*/

}
