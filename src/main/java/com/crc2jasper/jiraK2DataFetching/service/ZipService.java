package com.crc2jasper.jiraK2DataFetching.service;

import com.crc2jasper.jiraK2DataFetching.component.PromoReleaseEmailConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipService {
    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = PromoReleaseEmailConfig.getInstance();

    public static String getZipFilePath(){
        String zipFileName = PROMO_RELEASE_EMAIL_CONFIG.getYear() + "-" + PROMO_RELEASE_EMAIL_CONFIG.getBatch() + ".zip";
        String destDir = DirectoryService.getTempDestDirectory();
        String fullZipFilePath = destDir + "\\" + zipFileName;
        if (new File(fullZipFilePath).exists()){
            return fullZipFilePath;
        }else return "";
    }

    public static void compressFileToZip() {
        String srcDir = DirectoryService.getTempSrcDirectory();
        String destDir = DirectoryService.getTempDestDirectory();
        String zipFileName = PROMO_RELEASE_EMAIL_CONFIG.getYear() + "-" + PROMO_RELEASE_EMAIL_CONFIG.getBatch() + ".zip";
        try {
            File srcFile = new File(srcDir);
//            File destFile = new File(destDir);
            FileOutputStream fos = new FileOutputStream(destDir + "\\" + zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            addDirToArchive(zos, srcFile, srcFile.getName());
            zos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addDirToArchive(ZipOutputStream zos, File srcFile, String basePath) throws IOException {
        File[] files = srcFile.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addDirToArchive(zos, file, basePath + File.separator + file.getName());
                } else {
                    addToArchive(zos, file, basePath + File.separator + file.getName());
                }
            }
        }
    }

    private static void addToArchive(ZipOutputStream zos, File file, String entryName) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }
        zos.closeEntry();
        fis.close();
    }
}
