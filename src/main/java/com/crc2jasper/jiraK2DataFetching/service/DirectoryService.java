package com.crc2jasper.jiraK2DataFetching.service;

import com.crc2jasper.jiraK2DataFetching.component.PromoReleaseEmailConfig;
import com.crc2jasper.jiraK2DataFetching.config.SingletonConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class DirectoryService {
    private DirectoryService(){}
    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = PromoReleaseEmailConfig.getInstance();

    public static String getTempSrcDirectory() {
        String tempSrcDirectory = SingletonConfig.getIniInputPath() + "\\" + PROMO_RELEASE_EMAIL_CONFIG.getYear() + "-" + PROMO_RELEASE_EMAIL_CONFIG.getBatch();
        createDir(tempSrcDirectory);
        return tempSrcDirectory;
    }

    public static String getTempDestDirectory() {
        String tempDestDirectory = SingletonConfig.getIniInputPath() + "\\tempDestDir";
        createDir(tempDestDirectory);
        return tempDestDirectory;
    }

    private static void createDir(String inputPath){
        Path srcPath = Paths.get(inputPath);
        if (!Files.exists(srcPath)){
            try {
                Files.createDirectories(srcPath);
            } catch (Exception e) {
                System.out.println("Failed to create tempSrcDir directory.\n");
            }
        }
    }

    public static void delDir(){
        String tempSrcDirectory = SingletonConfig.getIniInputPath() + "\\" + PROMO_RELEASE_EMAIL_CONFIG.getYear() + "-" + PROMO_RELEASE_EMAIL_CONFIG.getBatch();
        String tempDestDirectory = SingletonConfig.getIniInputPath() + "\\tempDestDir";
        File srcDir = new File(tempSrcDirectory);
        File targetDir = new File(tempDestDirectory);
        deleteDirectory(srcDir);
        deleteDirectory(targetDir);
    }

    private static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteDirectory(file)) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

}
