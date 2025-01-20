package com.crc2jasper.jiraK2DataFetching.service;

import com.crc2jasper.jiraK2DataFetching.component.PromoReleaseEmailConfig;
import com.crc2jasper.jiraK2DataFetching.config.SingletonConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class AppIniService {
    private AppIniService(){}
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = PromoReleaseEmailConfig.getInstance();
    public static ObjectMapper getObjectMapper(){return AppIniService.OBJECT_MAPPER;}

    /*public static void startAPICall(){
        APIQueryService.fetchJiraUrgentServiceAPI();
    }*/

    public static void readJsonConfigFile(String[] args){
        File jsonFile;
        String jsonPath;
        if (args.length < 1 || !(jsonFile = new File(args[0] + "\\SetUpConfig.json")).isFile()){
            try (Scanner getInput = new Scanner(System.in)) {
                do {
                    System.out.print("\nNo valid json file identified for set up. Please enter the json file path again.\n(Make sure the json file is named \"SetUpConfig.json\")\n> ");
                    jsonPath = getInput.nextLine();
                    jsonFile = new File(jsonPath + "\\SetUpConfig.json");
                } while (!jsonFile.isFile());
            }
        }else jsonPath = args[0];

        SingletonConfig.setIniInputPath(jsonPath);

        try {
            // Read JSON file and convert to JsonNode
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonFile);
            SingletonConfig singletonConfig = OBJECT_MAPPER.treeToValue(rootNode, SingletonConfig.class);
            SingletonConfig.updateSingletonConfig(singletonConfig);
            System.out.println("\nJson data saved with the following:\n");
            System.out.println(SingletonConfig.getInstance());
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setupPromotionReleaseConfig(){
        EmailService.findLastBiweeklyPromotionRelease();
        String lastReleaseName = PROMO_RELEASE_EMAIL_CONFIG.getLastReleaseName();
        String lastReleaseDate = PROMO_RELEASE_EMAIL_CONFIG.getLastReleaseDate();
        if (lastReleaseName.isEmpty() || lastReleaseDate.isEmpty()){
            System.out.println("Failed to collect last promotion release info from mail box.");
            return;
        }
        System.out.printf("Successfully found and set up last promotion release: \"%s\" received on %s.\n", lastReleaseName, lastReleaseDate);
    }

    /*@Deprecated
    public static void rereadJsonConfig(){
        try {
            // Read JSON file and convert to JsonNode
            File jsonFile = SingletonConfig.getJsonFile();
            if (jsonFile.isFile()){
                JsonNode rootNode = OBJECT_MAPPER.readTree(jsonFile);
                SingletonConfig singletonConfig = OBJECT_MAPPER.treeToValue(rootNode, SingletonConfig.class);
                SingletonConfig.updateSingletonConfig(singletonConfig);
                System.out.println(SingletonConfig.getInstance().getEmailRecipients());
            }else return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

}
