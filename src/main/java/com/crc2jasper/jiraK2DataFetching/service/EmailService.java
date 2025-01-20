package com.crc2jasper.jiraK2DataFetching.service;

import com.crc2jasper.jiraK2DataFetching.component.*;
import com.crc2jasper.jiraK2DataFetching.config.SingletonConfig;
import com.crc2jasper.jiraK2DataFetching.util.TimeUtil;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

import java.util.Arrays;
import java.util.Map;

public class EmailService {
    private EmailService() {}
    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = PromoReleaseEmailConfig.getInstance();
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();

    public static void findLastBiweeklyPromotionRelease(){
        final ActiveXComponent axOutlook = new ActiveXComponent("Outlook.Application");
        // Initialize Outlook application
        Dispatch namespace = axOutlook.getProperty("Session").toDispatch();
        // Get the default Inbox folder
        Dispatch inbox = Dispatch.call(namespace, "GetDefaultFolder", 6).toDispatch(); // 6 is the constant for the Inbox folder

        Dispatch folders = Dispatch.call(inbox, "Folders").toDispatch();

        try{
            SingletonConfig singletonConfig = SingletonConfig.getInstance();
            Dispatch subfolder = Dispatch.call(folders, "Item", singletonConfig.getCmsBiweeklyReleaseFolder()).toDispatch();
            // Get items from the subfolder
            Dispatch items = Dispatch.call(subfolder, "Items").toDispatch();
            // !!!IMPORTANT!!! Sort items by received date in descending order
            // if not, GetLast won't work all the time
            Dispatch.call(items, "Sort", "ReceivedTime", false);
            Dispatch item = Dispatch.call(items, "GetLast").toDispatch();
            String subject = Dispatch.get(item, "Subject").toString();

            String receivedDate = TimeUtil.convertToDefaultDateFormat(Dispatch.get(item, "ReceivedTime").toString());
            PROMO_RELEASE_EMAIL_CONFIG.setLastReleaseName(subject).setLastReleaseDate(receivedDate).resetResendTmrStatus();
        }catch(IllegalStateException e){
            System.out.println("The target email folder is either empty or has finished reading all the email items.");
        }
    }

    public static boolean checkDailyForNewBiweeklyRelease(){
        // Initialize Outlook application
        ActiveXComponent axOutlook = new ActiveXComponent("Outlook.Application");
        Dispatch namespace = axOutlook.getProperty("Session").toDispatch();
        // Get the default Inbox folder
        Dispatch inbox = Dispatch.call(namespace, "GetDefaultFolder", 6).toDispatch(); // 6 is the constant for the Inbox folder
        // Get the subfolder named "Test1"
        Dispatch folders = Dispatch.call(inbox, "Folders").toDispatch();
        try{
            SingletonConfig singletonConfig = SingletonConfig.getInstance();
            Dispatch subfolder = Dispatch.call(folders, "Item", singletonConfig.getCmsBiweeklyReleaseFolder()).toDispatch();
            // Get items from the subfolder
            Dispatch items = Dispatch.call(subfolder, "Items").toDispatch();
            // !!!IMPORTANT!!! Sort items by received date in descending order
            // if not, GetLast won't work all the time
            Dispatch.call(items, "Sort", "ReceivedTime", false);
            Dispatch item = Dispatch.call(items, "GetLast").toDispatch();
            String subject = Dispatch.get(item, "Subject").toString();
            String sender = Dispatch.get(item, "SenderName").toString();
            String receivedDate = TimeUtil.convertToDefaultDateFormat(Dispatch.get(item, "ReceivedTime").toString());

            if (isValidNewBiweeklyRelease(subject, sender, receivedDate)){
                // has found new promotion -> kickstart other actions
                String[] subjectParts = subject.split(" ");
                final int YEAR_BATCH_IDX = 5;
                // e.g. ["[Production]:", "CMS", "Normal", "Release", "for", "2024-13", "-", "PRD"]
                // index:       0           1       2           3       4       5        6     7
                // make sure we have the required part and won't cause nullPointerException
                if (YEAR_BATCH_IDX < subjectParts.length && subjectParts[YEAR_BATCH_IDX].contains("-")){
                    String[] yearBatchParts = subjectParts[YEAR_BATCH_IDX].split("-");
                    String year = yearBatchParts[0];
                    String batch = yearBatchParts[1];
                    //update promotion release info
                    PROMO_RELEASE_EMAIL_CONFIG.setLastReleaseName(subject)
                            .setLastReleaseDate(receivedDate)
                            .setYear(year)
                            .setBatch(batch)
                            .resetResendTmrStatus();
                    return true;
                }
            }
            return false;
        }catch(IllegalStateException e){
            System.out.println("The target email folder is either empty or has finished reading all the email items.");
            return false;
        }
    }

    private static boolean isValidNewBiweeklyRelease(String subject, String sender, String receivedDate){
        return TimeUtil.checkIsToday(receivedDate)
                && TimeUtil.isAfter(PROMO_RELEASE_EMAIL_CONFIG.getLastReleaseDate(), receivedDate)
                && PROMO_RELEASE_EMAIL_CONFIG.isValidPromotionRelease(subject, sender)
                && PROMO_RELEASE_EMAIL_CONFIG.isDifferentRelease(subject);
    }

    public static void manualTestBiweeklyRelease(String lastReleaseDate, String year, String batch){
        String subject = String.format("[Production]: CMS Normal Release for %s-%s - PRD", year, batch);
        PROMO_RELEASE_EMAIL_CONFIG.setLastReleaseName(subject)
                .setLastReleaseDate(lastReleaseDate)
                .setYear(year)
                .setBatch(batch)
                .resetResendTmrStatus();
    }

    public static boolean checkdailyForNewBiweeklyReleaseSimulation(){
        //simulate that this is the last release item saved in the system
        PROMO_RELEASE_EMAIL_CONFIG.setLastReleaseName("[Production]: CMS Normal Release for 2024-01 - PRD")
                .setLastReleaseDate("01-Jan-2024")
                .setYear("2024")
                .setBatch("01")
                .resetResendTmrStatus();

        // Initialize Outlook application
        ActiveXComponent axOutlook = new ActiveXComponent("Outlook.Application");
        Dispatch namespace = axOutlook.getProperty("Session").toDispatch();
        // Get the default Inbox folder
        Dispatch inbox = Dispatch.call(namespace, "GetDefaultFolder", 6).toDispatch(); // 6 is the constant for the Inbox folder
        // Get the subfolder named "Test1"
        Dispatch folders = Dispatch.call(inbox, "Folders").toDispatch();

        try{
            Dispatch subfolder = Dispatch.call(folders, "Item", "_CMS Normal Release").toDispatch();

            // Get items from the subfolder
            Dispatch items = Dispatch.call(subfolder, "Items").toDispatch();

            // !!!IMPORTANT!!! Sort items by received date in descending order
            // if not, GetLast won't work all the time
            Dispatch.call(items, "Sort", "ReceivedTime", false);

            Dispatch item = Dispatch.call(items, "GetLast").toDispatch();
            String subject = Dispatch.get(item, "Subject").toString();
            String sender = Dispatch.get(item, "SenderName").toString();
            String receivedDate = TimeUtil.convertToDefaultDateFormat(Dispatch.get(item, "ReceivedTime").toString());

            if (TimeUtil.isAfter(PROMO_RELEASE_EMAIL_CONFIG.getLastReleaseDate(), receivedDate)
                    && PROMO_RELEASE_EMAIL_CONFIG.isValidPromotionRelease(subject, sender)
                    && PROMO_RELEASE_EMAIL_CONFIG.isDifferentRelease(subject)){
                // has found new promotion -> kickstart other actions

                String[] subjectParts = subject.split(" ");

                final int yearBatchIdx = 5;
                // e.g. ["[Production]:", "CMS", "Normal", "Release", "for", "2024-13", "-", "PRD"]
                // index:       0           1       2           3       4       5        6     7

                // make sure we have the required part and won't cause nullPointerException
                if (yearBatchIdx < subjectParts.length && subjectParts[yearBatchIdx].contains("-")){
                    String[] yearBatchParts = subjectParts[yearBatchIdx].split("-");
                    String year = yearBatchParts[0];
                    String batch = yearBatchParts[1];

                    //update promotion release info
                    PROMO_RELEASE_EMAIL_CONFIG.setLastReleaseName(subject)
                            .setLastReleaseDate(receivedDate)
                            .setYear(year)
                            .setBatch(batch)
                            .resetResendTmrStatus();
                    return true;
                }
            }
            return false;
        }catch(IllegalStateException e){
            System.out.println("The target email folder is either empty or has finished reading all the email items.");
            return false;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    public static void sendUrgentServiceEmail_V2(){
        Dispatch mail = Dispatch
                .invoke(new ActiveXComponent("Outlook.Application"),
                        "CreateItem",
                        Dispatch.Get,
                        new Object[]{"0"},
                        new int[0])
                .toDispatch();

        SingletonConfig singletonConfig = SingletonConfig.getInstance();
        Dispatch.put(mail, "Subject", singletonConfig.getEmailSubjectUrgentService());
        Dispatch.put(mail, "To", singletonConfig.getEmailRecipients());

        ////////////////////////////////////////////
        Map<String, PromoForm> keyPromoFormMap = DATA_CENTER.getKeyPromoFormMap();
        String finalContent = EmailHTML.compileEmailHTMLContent(keyPromoFormMap, true);
        Dispatch.put(mail, "HTMLBody", finalContent);
        //////////////////////////////////////////

        // Set reminder properties
        Dispatch.put(mail, "ReminderSet", true);
        Dispatch.call(mail, "Send");
    }

    public static void sendBiweeklyEmailWithAttachment_V2(){
        Dispatch mail = Dispatch
                .invoke(new ActiveXComponent("Outlook.Application"),
                        "CreateItem",
                        Dispatch.Get,
                        new Object[]{"0"},
                        new int[0])
                .toDispatch();

        SingletonConfig singletonConfig = SingletonConfig.getInstance();
        Dispatch.put(mail, "Subject", singletonConfig.getEmailSubjectBiweekly());
        Dispatch.put(mail, "To", singletonConfig.getEmailRecipients());

        ////////////////////////////////////////////
        Map<String, PromoForm> keyPromoFormMap = DATA_CENTER.getKeyPromoFormMap();
        String finalContent = EmailHTML.compileEmailHTMLContent(keyPromoFormMap, false);
        Dispatch.put(mail, "HTMLBody", finalContent);
        //////////////////////////////////////////

        // Attach a document
        Dispatch attachments = Dispatch.get(mail, "Attachments").toDispatch();
        String zipFilePath = ZipService.getZipFilePath();
        if (!zipFilePath.isBlank()){
            Dispatch.call(attachments, "Add", zipFilePath);
        }

        // Set reminder properties
        Dispatch.put(mail, "ReminderSet", true);
        Dispatch.call(mail, "Send");
    }

      /*@Deprecated
    public static void sendUrgentServiceEmail(){
        final ActiveXComponent axOutlook = new ActiveXComponent("Outlook.Application");
        Dispatch oOutlook = axOutlook.getObject();
        Dispatch mail = Dispatch
                .invoke(oOutlook,
                        "CreateItem",
                        Dispatch.Get,
                        new Object[]{"0"},
                        new int[0])
                .toDispatch();

        Dispatch.put(mail, "Subject", singletonConfig.getEmailSubjectUrgentService());
        Dispatch.put(mail, "To", singletonConfig.getEmailRecipients());

        StringBuilder relatedTableContent = new StringBuilder();
        StringBuilder unrelatedTableContent = new StringBuilder();
        if(!allFormData.getAllRelatedEmailForms().isEmpty()){
            Set<String> allRelatedFormKeys = allFormData.getAllRelatedEmailForms().keySet();
            for (String key: allRelatedFormKeys){
                relatedTableContent.append(EmailHTML.genTableRowContent(allFormData.getRelatedEmailForm(key), true));
            }
        }
        if(!allFormData.getAllUnrelatedEmailForms().isEmpty()){
            Set<String> allUnrelatedFormKeys = allFormData.getAllUnrelatedEmailForms().keySet();
            for (String key: allUnrelatedFormKeys){
                unrelatedTableContent.append(EmailHTML.genTableRowContent(allFormData.getUnrelatedEmailForm(key), true));
            }
        }

        int relatedCnt = allFormData.getAllRelatedEmailForms().size(), unrelatedCnt = allFormData.getAllUnrelatedEmailForms().size();
//        String finalContent = EmailHTML.emailHTMLDom(relatedCnt, relatedTableContent.toString(), unrelatedCnt, unrelatedTableContent.toString());
        String finalContent = EmailHTML.dynamicEmailHTMLDom(
                relatedCnt,
                relatedTableContent.toString(),
                unrelatedCnt, unrelatedTableContent.toString(),
                true);

        Dispatch.put(mail, "HTMLBody", finalContent);
        // Set reminder properties
        Dispatch.put(mail, "ReminderSet", true);
        Dispatch.call(mail, "Send");
    }

    @Deprecated
    public static void sendBiweeklyEmail(){
        final ActiveXComponent axOutlook = new ActiveXComponent("Outlook.Application");
        Dispatch oOutlook = axOutlook.getObject();
        Dispatch mail = Dispatch
                .invoke(oOutlook,
                        "CreateItem",
                        Dispatch.Get,
                        new Object[]{"0"},
                        new int[0])
                .toDispatch();

        Dispatch.put(mail, "Subject", singletonConfig.getEmailSubjectBiweekly());
        Dispatch.put(mail, "To", singletonConfig.getEmailRecipients());

        StringBuilder relatedTableContent = new StringBuilder();
        StringBuilder unrelatedTableContent = new StringBuilder();
        if(!allFormData.getAllRelatedEmailForms().isEmpty()){
            Set<String> allRelatedFormKeys = allFormData.getAllRelatedEmailForms().keySet();
            for (String key: allRelatedFormKeys){
                relatedTableContent.append(EmailHTML.genTableRowContent(allFormData.getRelatedEmailForm(key), false));
            }
        }
        if(!allFormData.getAllUnrelatedEmailForms().isEmpty()){
            Set<String> allUnrelatedFormKeys = allFormData.getAllUnrelatedEmailForms().keySet();
            for (String key: allUnrelatedFormKeys){
                unrelatedTableContent.append(EmailHTML.genTableRowContent(allFormData.getUnrelatedEmailForm(key), false));
            }
        }

        int relatedCnt = allFormData.getAllRelatedEmailForms().size(), unrelatedCnt = allFormData.getAllUnrelatedEmailForms().size();
//        String finalContent = EmailHTML.emailHTMLDom(relatedCnt, relatedTableContent.toString(), unrelatedCnt, unrelatedTableContent.toString());
        String finalContent = EmailHTML.dynamicEmailHTMLDom(
                relatedCnt,
                relatedTableContent.toString(),
                unrelatedCnt, unrelatedTableContent.toString(),
                false);
        Dispatch.put(mail, "HTMLBody", finalContent);

        // Attach a document
        Dispatch attachments = Dispatch.get(mail, "Attachments").toDispatch();
        String zipFilePath = ZipService.getZipFilePath();
        if (!zipFilePath.isBlank()){
            Dispatch.call(attachments, "Add", zipFilePath);
        }

        // Set reminder properties
        Dispatch.put(mail, "ReminderSet", true);
        Dispatch.call(mail, "Send");
    }*/
}
