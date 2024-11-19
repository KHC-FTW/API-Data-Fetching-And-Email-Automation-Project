package com.crc2jasper.jiraK2DataFetching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

public class SystemIni {
    private SystemIni(){}
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SingletonConfig singletonConfig = SingletonConfig.getInstance();
    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();

    public static ObjectMapper getObjectMapper(){return SystemIni.objectMapper;}

    public static void startAPICall(String jiraAPI){
        APIQueryService.fetchJiraAPI(jiraAPI);
    }

    public static void readJsonConfigFile(String[] args){
        File jsonFile;
        if (args.length < 1 || !(jsonFile = new File(args[0] + "\\SetUpConfig.json")).isFile()){
            Scanner getInput = new Scanner(System.in);
            String jsonPath;
            do{
                System.out.print("\nNo valid json file identified for set up. Please enter the json file path again.\n(Make sure the json file is named \"SetUpConfig.json\")\n> ");
                jsonPath = getInput.nextLine();
                jsonFile = new File(jsonPath + "\\SetUpConfig.json");
            }while(!jsonFile.isFile());
        }

        SingletonConfig.setJsonFile(jsonFile);

        try {
            // Read JSON file and convert to JsonNode
            JsonNode rootNode = objectMapper.readTree(jsonFile);
            SingletonConfig singletonConfig = objectMapper.treeToValue(rootNode, SingletonConfig.class);
            SingletonConfig.updateSingletonConfig(singletonConfig);
            System.out.println("\nJson data saved with the following:\n");
            System.out.println(SingletonConfig.getInstance());
            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setupPromotionReleaseConfig(){
        EmailService.findLastPromotionRelease();
        String lastReleaseName = promotionRelease.getLastReleaseName();
        String lastReleaseDate = promotionRelease.getLastReleaseDate();
        if (lastReleaseName.isEmpty() || lastReleaseDate.isEmpty()){
            System.out.println("Failed to collect last promotion release info from mail box.");
            return;
        }
        System.out.printf("Successfully found and set up last promotion release: \"%s\" received on %s.\n", lastReleaseName, lastReleaseDate);
    }

    @Deprecated
    public static void rereadJsonConfig(){
        try {
            // Read JSON file and convert to JsonNode
            File jsonFile = SingletonConfig.getJsonFile();
            if (jsonFile.isFile()){
                JsonNode rootNode = objectMapper.readTree(jsonFile);
                SingletonConfig singletonConfig = objectMapper.treeToValue(rootNode, SingletonConfig.class);
                SingletonConfig.updateSingletonConfig(singletonConfig);
                System.out.println(SingletonConfig.getInstance().getEmailRecipients());
            }else return;
//            System.out.println("Json data saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
