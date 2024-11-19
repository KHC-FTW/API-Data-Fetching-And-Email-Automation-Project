package com.crc2jasper.jiraK2DataFetching;

import java.util.List;
import java.util.ArrayList;

public class EmailConfig {
    private EmailConfig(){}
    private static String subject;
    private static List<String> recipients = new ArrayList<>();

    public static void setSubject(String subject){
        EmailConfig.subject = subject;
    }

    public static void setRecipients(ArrayList<String> recipients){
        EmailConfig.recipients = recipients;
    }

    public static String getSubject(){return subject;}
    public static List<String> getRecipients(){return recipients;}
}
