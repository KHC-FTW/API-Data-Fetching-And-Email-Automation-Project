package com.crc2jasper.jiraK2DataFetching;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class EmailScheduler {
    private static final PromotionRelease PROMOTION_RELEASE = PromotionRelease.getInstance();
    private static final AllFormData ALL_FORM_DATA = AllFormData.getInstance();
    private static final TextSummary TEXT_SUMMARY = TextSummary.getInstance();
    private static final CRInfo CR_INFO = CRInfo.getInstance();

    private static final DataCenter DATA_CENTER = DataCenter.getInstance();


//    @Scheduled(cron = "*/10 * * * * *")

    // Start at 17:00 every day
//    @Scheduled(cron = "0 0 17 * * *")
//    @Scheduled(cron = "*/10 * * * * *")
    public static void sendUrgentServiceEmail(){
        ALL_FORM_DATA.clearAllEmailFormData();
        APIQueryService.fetchJiraUrgentServiceAPI();
        // Exception may be due to parameter passing, try not to pass parameter
        // Current naive mitigation: all methods can't have parameters, must do it in the method itself
        EmailService.sendUrgentServiceEmail();
        ALL_FORM_DATA.clearAllEmailFormData();
    }

    // Check at 17:30 every day
    @Scheduled(cron = "0 30 17 * * *")
    public static void sendBiweeklyEmail(){
        if(EmailService.checkDailyForNewBiweeklyRelease()){
            eventSequenceBiweekly();
            // Set the flag to resend the email tomorrow
            PROMOTION_RELEASE.setToResendTmr();
        }
    }

    // Check at 09:00 every day
    @Scheduled(cron = "0 0 9 * * *")
    public static void resendBiweeklyEmail(){
        if(PROMOTION_RELEASE.getResendTmrStatus()){
            eventSequenceBiweekly();
            // Disable resend
            PROMOTION_RELEASE.resetResendTmrStatus();
        }
    }

//    @Deprecated
//    @Scheduled(cron = "*/10 * * * * *")
    public static void simulateSendBiweeklyEmail(){
        if(EmailService.dailyCheckNewReleaseSimulation()){
            eventSequenceBiweekly();
            // Set the flag to resend the email tomorrow
            PROMOTION_RELEASE.setToResendTmr();
        }
    }

    public static void manualTestSendBiweeklyEmail(String lastReleaseDate, String year, String batch){
        EmailService.manualTestBiweeklyRelease(lastReleaseDate, year, batch);
        eventSequenceBiweekly();
    }

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
    }


    /////////////////////////// V2 ///////////////////////////

    public static void sendUrgentServiceEmail_V2(){
        DATA_CENTER.clearAllData();
        APIQueryService.fetchJiraUrgentServiceAPI_V2();
        EmailService.sendUrgentServiceEmail_V2();
        DATA_CENTER.clearAllData();
    }

    public static void manualTestSendBiweeklyEmail_V2_multiThreaded(String lastReleaseDate, String year, String batch){
        EmailService.manualTestBiweeklyRelease(lastReleaseDate, year, batch);
        eventSequenceBiweekly_V2_multiThreaded();
    }

    public static void manualTestSendBiweeklyEmail_V2(String lastReleaseDate, String year, String batch){
        EmailService.manualTestBiweeklyRelease(lastReleaseDate, year, batch);
        eventSequenceBiweekly_V2();
    }

    private static void eventSequenceBiweekly_V2_multiThreaded(){

        long startTime = System.nanoTime();

        DATA_CENTER.clearAllData();

        /*Mono<String> biweeklyApiCall = APIQueryService.fetchJiraBiweeklyAPI_V2_reactive();
        Mono<String> urgentServiceApiCall = APIQueryService.fetchJiraUrgentServiceForBiweeklyAPI_V2_reactive();


        Mono.zip(biweeklyApiCall, urgentServiceApiCall)
                .then(Mono.fromRunnable(() -> {
                    ReadmeService.createReadmeFile(ReadmeService.genReadmeContent());
                    UrlService.genUrlFiles_V2();
                }))
                .then(Mono.fromRunnable(ZipService::compressFileToZip))
                .then(Mono.fromRunnable(EmailService::sendBiweeklyEmailWithAttachment_V2))
                .then(Mono.fromRunnable(DATA_CENTER::clearAllData))
                .then(Mono.fromRunnable(DirectoryService::delDir))
                .doOnTerminate(() -> {
                    long endTime = System.nanoTime();
                    long duration = (endTime - startTime) / 1_000_000_000; // Convert to seconds
                    System.out.println("eventSequenceBiweekly_V2_multiThreaded() execution time: " + duration + " seconds");
                })
                .subscribe();*/



        Runnable r1 = APIQueryService::fetchJiraBiweeklyAPI_V2;
        Runnable r2 = APIQueryService::fetchJiraUrgentServiceForBiweeklyAPI_V2;

        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Runnable r3 = () -> ReadmeService.createReadmeFile(ReadmeService.genReadmeContent());
        Runnable r4 = UrlService::genUrlFiles_V2;

        Thread t3 = new Thread(r3);
        Thread t4 = new Thread(r4);
        t3.start();
        t4.start();
        try {
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ZipService.compressFileToZip();
        EmailService.sendBiweeklyEmailWithAttachment_V2();
        DATA_CENTER.clearAllData();
        DirectoryService.delDir();

        /////////////////////////////////////////////
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000_000; // Convert to seconds
        System.out.println("eventSequenceBiweekly_V2_multiThreaded() execution time: " + duration + " seconds");
    }

    private static void eventSequenceBiweekly_V2(){

        long startTime = System.nanoTime();

        DATA_CENTER.clearAllData();
        APIQueryService.fetchJiraBiweeklyAPI_V2();
        APIQueryService.fetchJiraUrgentServiceForBiweeklyAPI_V2();
        ReadmeService.createReadmeFile(ReadmeService.genReadmeContent());
        UrlService.genUrlFiles_V2();
        ZipService.compressFileToZip();
        EmailService.sendBiweeklyEmailWithAttachment_V2();
        DATA_CENTER.clearAllData();
        DirectoryService.delDir();

        /////////////////////////////////////////////
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000_000; // Convert to seconds
        System.out.println("eventSequenceBiweekly_V2() execution time: " + duration + " seconds");
    }

}
