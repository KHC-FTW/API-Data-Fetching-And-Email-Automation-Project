package com.crc2jasper.jiraK2DataFetching.config;

import com.crc2jasper.jiraK2DataFetching.component.PromoReleaseEmailConfig;
import com.crc2jasper.jiraK2DataFetching.util.TimeUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SingletonConfig {
    private SingletonConfig(){}
    private static SingletonConfig singletonAPIConfig = new SingletonConfig();
    @JsonIgnore
    private static String iniInputPath;
//    private static File jsonFile;
    public static SingletonConfig getInstance(){return singletonAPIConfig;}
    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = PromoReleaseEmailConfig.getInstance();

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
        private String jql_biweekly_urgent_service;
        private String jiraFields;
        private String jfrogAPI;

        @Override
        public String toString(){
            return "\njiraAPI: " + jiraAPI
                    + "\njql_urgent_service: " + jql_urgent_service
                    + "\njql_biweekly_prn: " + jql_biweekly_prn
                    + "\njql_biweekly_urgent_service: " + jql_biweekly_urgent_service
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
    @Getter
    private List<String> hospList = new ArrayList<>();

    @Override
    public String toString(){
        return "Admin: " + admin
                + "\n\nAPIConfig: " + apiConfig
                + "\n\nEmailConfig: " + emailConfig
                + "\n\nHospList: " + hospList;
    }

    /*public static void setJsonFile(File jsonFile){SingletonConfig.jsonFile = jsonFile;}
    public static File getJsonFile(){return SingletonConfig.jsonFile;}*/
    public static void setIniInputPath(String path){SingletonConfig.iniInputPath = path;}
    public static String getIniInputPath(){return SingletonConfig.iniInputPath;}

    public static void updateSingletonConfig(SingletonConfig newSingletonConfig){
        singletonAPIConfig = newSingletonConfig;
    }


    public String getAdminUsername(){return admin.username;}
    public String getAdminPassword(){return admin.password;}
    public String getAdminEmail(){return admin.email;}

    public String getJiraRestAPI(){return apiConfig.jiraAPI;}
    public String getFullJiraAPIUrgentService(){return apiConfig.jiraAPI + apiConfig.jql_urgent_service + apiConfig.jiraFields;}
    public String getRawJiraAPIBiweeklyPrn(){return apiConfig.jiraAPI + apiConfig.jql_biweekly_prn + apiConfig.jiraFields;}
    public String getFullJiraAPIBiweeklyPrn(){
        // https://hatool.home/jira/rest/api/2/search?jql=
        // + project = ITOCMS AND summary ~ "PPM%s*" OR (summary ~ "PRN%s*" AND created >= -60d) AND "Promotion Schedule" is not EMPTY ORDER BY summary
        // &maxResults=1000&fields=customfield_11400&fields=summary&fields=description&fields=customfield_11628&fields=status&fields=customfield_10519&fields=customfield_14500&fields=customfield_11887
        String year = PROMO_RELEASE_EMAIL_CONFIG.getYear();   //e.g. 2024
        String year_batch = year + "_" + PROMO_RELEASE_EMAIL_CONFIG.getBatch();   //e.g. 2024_13
        return apiConfig.jiraAPI + String.format(apiConfig.jql_biweekly_prn, year_batch, year) + apiConfig.jiraFields;
    }
    public String getFullJiraAPIUrgentServiceForBiweekly(){
        String year = PROMO_RELEASE_EMAIL_CONFIG.getYear();
        String year_batch = year + "-" + PROMO_RELEASE_EMAIL_CONFIG.getBatch();
        String unresolvedPeriodBeginDate = TimeUtil.calculateDate(PROMO_RELEASE_EMAIL_CONFIG.getLastReleaseDate(), -7, "dd-MMM-yyyy", "yyyy-MM-dd");
        return apiConfig.jiraAPI + String.format(apiConfig.jql_biweekly_urgent_service, year_batch, year_batch, year, unresolvedPeriodBeginDate) + apiConfig.jiraFields;
    }
    public String getJfrogAPI(){return apiConfig.jfrogAPI;}

    public String getEmailSubjectUrgentService(){return emailConfig.subject_urgent_service;}
    public String getEmailSubjectBiweekly(){
        String year_batch = PROMO_RELEASE_EMAIL_CONFIG.getYear() + "-" + PROMO_RELEASE_EMAIL_CONFIG.getBatch();
        return String.format(emailConfig.subject_biweekly, year_batch);
    }
    public String getCmsBiweeklyReleaseFolder(){return emailConfig.cms_biweekly_release_folder;}
    public String getEmailRecipients(){return emailConfig.recipients;}

    public String getJiraFields(){return apiConfig.jiraFields;}

}
