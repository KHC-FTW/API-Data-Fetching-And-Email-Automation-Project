package com.crc2jasper.jiraK2DataFetching;


public class AdminConfig {

    private static String username;
    private static String password;
    private static String email;
    private AdminConfig(){}

    public static void setUsername(String username) {
        AdminConfig.username = username;
    }

    public static void setPassword(String password) {
        AdminConfig.password = password;
    }

    public static void setEmail(String email){
        AdminConfig.email = email;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }

    public static String getEmail() {
        return email;
    }

    public static String displayAdminInfo(){
        return "Admin info:\nUsername: " + username
                + "\nPassword: " + password
                + "\nEmail: " + email;
    }

}
