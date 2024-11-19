package com.crc2jasper.jiraK2DataFetching;


public class APIConfig {

    private static String jiraAPI;
    private static String jFrogAPI;

    private APIConfig(){}

    public static void setjFrogAPI(String jFrogAPI) {
        APIConfig.jFrogAPI = jFrogAPI;
    }

    public static void setJiraAPI(String jiraAPI) {
        APIConfig.jiraAPI = jiraAPI;
    }

    public static String getJiraAPI() {
        return jiraAPI;
    }

    public static String getjFrogAPI() {
        return jFrogAPI;
    }


}
