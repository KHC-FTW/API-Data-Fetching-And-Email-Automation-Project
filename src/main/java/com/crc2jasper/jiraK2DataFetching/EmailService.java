package com.crc2jasper.jiraK2DataFetching;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;

import java.util.Set;

public class EmailService {

//    private static final String emailSubject = SingletonConfig.getInstance().getEmailSubjectUrgentService();
    private static final SingletonConfig singletonConfig = SingletonConfig.getInstance();
    private static final AllFormData allFormData = AllFormData.getInstance();
    private static final ActiveXComponent axOutlook = new ActiveXComponent("Outlook.Application");
    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();

    public static void sendUrgentServiceEmail(){

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
                relatedTableContent.append(EmailHTML.genTableContent(allFormData.getRelatedEmailForm(key)));
            }
        }
        if(!allFormData.getAllUnrelatedEmailForms().isEmpty()){
            Set<String> allUnrelatedFormKeys = allFormData.getAllUnrelatedEmailForms().keySet();
            for (String key: allUnrelatedFormKeys){
                unrelatedTableContent.append(EmailHTML.genTableContent(allFormData.getUnrelatedEmailForm(key)));
            }
        }

        int relatedCnt = allFormData.getAllRelatedEmailForms().size(), unrelatedCnt = allFormData.getAllUnrelatedEmailForms().size();
        String finalContent = EmailHTML.emailHTMLDom(relatedCnt, relatedTableContent.toString(), unrelatedCnt, unrelatedTableContent.toString());

        Dispatch.put(mail, "HTMLBody", finalContent);
        // Set reminder properties
        Dispatch.put(mail, "ReminderSet", true);
        Dispatch.call(mail, "Send");
    }

    public static void sendBiweeklyEmail(){

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
                relatedTableContent.append(EmailHTML.genTableContent(allFormData.getRelatedEmailForm(key)));
            }
        }
        if(!allFormData.getAllUnrelatedEmailForms().isEmpty()){
            Set<String> allUnrelatedFormKeys = allFormData.getAllUnrelatedEmailForms().keySet();
            for (String key: allUnrelatedFormKeys){
                unrelatedTableContent.append(EmailHTML.genTableContent(allFormData.getUnrelatedEmailForm(key)));
            }
        }

        int relatedCnt = allFormData.getAllRelatedEmailForms().size(), unrelatedCnt = allFormData.getAllUnrelatedEmailForms().size();
        String finalContent = EmailHTML.emailHTMLDom(relatedCnt, relatedTableContent.toString(), unrelatedCnt, unrelatedTableContent.toString());

        Dispatch.put(mail, "HTMLBody", finalContent);

        // Attach a document
        Dispatch attachments = Dispatch.get(mail, "Attachments").toDispatch();
        Dispatch.call(attachments, "Add", SingletonConfig.getIniInputPath() + "\\Readme.txt");

        // Set reminder properties
        Dispatch.put(mail, "ReminderSet", true);
        Dispatch.call(mail, "Send");
    }

    public static void findLastPromotionRelease(){

        // Initialize Outlook application
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

            String receivedDate = TimeUtil.convertToDefaultDateFormat(Dispatch.get(item, "ReceivedTime").toString());
            promotionRelease.setLastReleaseName(subject).setLastReleaseDate(receivedDate).resetResendTmrStatus();

        }catch(IllegalStateException e){
            System.out.println("The target email folder is either empty or has finished reading all the email items.");
        }
    }

    public static boolean dailyCheckForNewRelease(){
        // Initialize Outlook application
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

            if (TimeUtil.checkIsToday(receivedDate)
                    && TimeUtil.isAfter(promotionRelease.getLastReleaseDate(), receivedDate)
                    && promotionRelease.isValidPromotionRelease(subject, sender)
                    && promotionRelease.isDifferentRelease(subject)){
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
                    promotionRelease.setLastReleaseName(subject)
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

    @Deprecated
    public static boolean dailyCheckNewReleaseSimulation(){
        //simulate that this is the last release item saved in the system
        promotionRelease.setLastReleaseName("[Production]: CMS Normal Release for 2024-01 - PRD")
                .setLastReleaseDate("01-Jan-2024")
                .setYear("2020")
                .setBatch("01")
                .resetResendTmrStatus();


        // Initialize Outlook application
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

            if (TimeUtil.isAfter(promotionRelease.getLastReleaseDate(), receivedDate)
                    && promotionRelease.isValidPromotionRelease(subject, sender)
                    && promotionRelease.isDifferentRelease(subject)){
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
                    promotionRelease.setLastReleaseName(subject)
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
}
