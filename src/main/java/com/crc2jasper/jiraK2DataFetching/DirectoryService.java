package com.crc2jasper.jiraK2DataFetching;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryService {
    private DirectoryService(){}
    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();
    private static final String TEMP_SRC_DIRECTORY = SingletonConfig.getIniInputPath() + "\\" + promotionRelease.getYear() + "-" + promotionRelease.getBatch();
    private static final String TEMP_DEST_DIRECTORY = SingletonConfig.getIniInputPath() + "\\tempDestDir";

    public static String getTempSrcDirectory() {
        createDir(TEMP_SRC_DIRECTORY);
        return TEMP_SRC_DIRECTORY;
    }

    public static String getTempDestDirectory() {
        createDir(TEMP_DEST_DIRECTORY);
        return TEMP_DEST_DIRECTORY;
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
        File srcDir = new File(TEMP_SRC_DIRECTORY);
        File targetDir = new File(TEMP_DEST_DIRECTORY);
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
