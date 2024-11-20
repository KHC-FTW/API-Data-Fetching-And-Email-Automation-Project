package com.crc2jasper.jiraK2DataFetching;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class SingletonConfig {
    private SingletonConfig(){}
    private static SingletonConfig singletonAPIConfig = new SingletonConfig();
    private static File jsonFile;
    public static SingletonConfig getInstance(){return singletonAPIConfig;}
    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();

    @Getter
    @Setter
    @NoArgsConstructor
    private class Admin{
        private String username;
        private String password;
        private String email;

        @Override
        public String toString(){
            return "\nusername: " + username
                    +"\npassword: " + "*".repeat(password.length())
                    +"\nemail: " + email;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private class APIConfig{
        private String jiraAPI;
        private String jql_urgent_service;
        private String jql_biweekly_prn;
        private String jiraFields;
        private String jfrogAPI;

        @Override
        public String toString(){
            return "\njiraAPI: " + jiraAPI
                    + "\njql_urgent_service: " + jql_urgent_service
                    + "\njql_biweekly_prn: " + jql_biweekly_prn
                    + "\njiraFields: " + jiraFields
                    + "\njfrogAPI: " + jfrogAPI;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private class EmailConfig{
        private String subject_urgent_service;
        private String subject_biweekly;
        private String cms_biweekly_release_folder;
        private String recipients;

        @Override
        public String toString(){
            return "\nsubject_urgent_service: " + subject_urgent_service
                    +"\nsubject_biweekly: " + subject_biweekly
                    + "\ncms_biweekly_release_folder: " + cms_biweekly_release_folder
                    + "\nrecipients: " + recipients;
        }
    }

    private Admin admin = new Admin();
    private APIConfig apiConfig = new APIConfig();
    private EmailConfig emailConfig = new EmailConfig();

    @Override
    public String toString(){
        return "Admin: " + admin
                + "\n\nAPIConfig: " + apiConfig
                + "\n\nEmailConfig: " + emailConfig;
    }

    public static void setJsonFile(File jsonFile){SingletonConfig.jsonFile = jsonFile;}
    public static File getJsonFile(){return SingletonConfig.jsonFile;}

    public static void updateSingletonConfig(SingletonConfig newSingletonConfig){
        singletonAPIConfig = newSingletonConfig;
    }


    public String getAdminUsername(){return admin.username;}
    public String getAdminPassword(){return admin.password;}
    public String getAdminEmail(){return admin.email;}

    public String getFullJiraAPIUrgentService(){return apiConfig.jiraAPI + apiConfig.jql_urgent_service + apiConfig.jiraFields;}
    public String getRawJiraAPIBiweeklyPrn(){return apiConfig.jiraAPI + apiConfig.jql_biweekly_prn + apiConfig.jiraFields;}
    public String getFullJiraAPIBiweeklyPrn(){
        String year = promotionRelease.getYear();   //e.g. 2024
        String year_batch = year + "_" + promotionRelease.getBatch();   //e.g. 2024_13
        return apiConfig.jiraAPI + String.format(apiConfig.jql_biweekly_prn, year_batch, year) + apiConfig.jiraFields;
    }
    public String getJfrogAPI(){return apiConfig.jfrogAPI;}

    public String getEmailSubjectUrgentService(){return emailConfig.subject_urgent_service;}
    public String getEmailSubjectBiweekly(){
        String year_batch = promotionRelease.getYear() + "-" + promotionRelease.getBatch();
        return String.format(emailConfig.subject_biweekly, year_batch);
    }
    public String getCmsBiweeklyReleaseFolder(){return emailConfig.cms_biweekly_release_folder;}
    public String getEmailRecipients(){return emailConfig.recipients;}

}
