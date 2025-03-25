package com.crc2jasper.jiraK2DataFetching.service;

import com.crc2jasper.jiraK2DataFetching.component.DataCenter;
import com.crc2jasper.jiraK2DataFetching.component.PromoForm;
import com.crc2jasper.jiraK2DataFetching.component.PromoReleaseEmailConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompileSeqService {
    private CompileSeqService(){}
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();
    private static final PromoReleaseEmailConfig PROMO_RELEASE_EMAIL_CONFIG = PromoReleaseEmailConfig.getInstance();
    private static final String COMMENT_FORMAT = "/* %s */";
    private static final String CONTENT_STRUCTURE = """
            /* ========================================================================== */
            /* Part 1: Forwarder Bi-weekly %s */
            /* ========================================================================== */
            
            %s
            /* ========================================================================== */
            /* Part 2: Backend Script Bi-weekly %s */
            /* ========================================================================== */
            
            %s
            /* ========================================================================== */
            /* Part 3: Special arrangement for Urgent/Service Promotion %s */
            /* ========================================================================== */
            
            %s
            /* ========================================================================== */
            /* Part 4: Package */
            /* ========================================================================== */
            
            
            """;

    public static String genCompileSeqContent(String part1, String part2, String part3){
        final String YEAR_BATCH = PROMO_RELEASE_EMAIL_CONFIG.getYear() + "-" +PROMO_RELEASE_EMAIL_CONFIG.getBatch();
        return String.format(CONTENT_STRUCTURE, YEAR_BATCH,
                part1, YEAR_BATCH,
                part2, YEAR_BATCH, part3);
    }

    public static boolean createCompileSeqFile(String content){
        Path path = Paths.get(DirectoryService.getTempSrcDirectory() + "\\compile_seq.txt");
        if(!Files.exists(path)){
            try {
                Files.createFile(path);
            } catch (Exception e) {
                System.out.println("Failed to create Readme.txt file.\n");
                return false;
            }
        }
        try {
            Files.writeString(path, content);
        } catch (Exception e) {
            System.out.println("Failed to write to Readme.txt file.\n");
            return false;
        }
        return true;
    }

    public static String compilePart1Forwarder(){
        StringBuilder content = new StringBuilder();
        DATA_CENTER.getKeyPromoFormMap().values().stream()
                .filter(promoForm -> promoForm.isImpHospOrImpCorp() && promoForm.isActivePromotion())
                .forEach(promoForm -> {
                    String ppmAndTicket = String.format(COMMENT_FORMAT, promoForm.getSummary() + "\t" + String.join(", ", promoForm.getAllTickets()));
                    String specialAffectedHosp = "";
                    String affectedHosp = promoForm.getAffectedHosp();
                    if (!affectedHosp.isBlank()){
                        String[] parts = affectedHosp.split("\n");
                        int firstValidIndex = 0;
                        for (int i = 0; i < parts.length; i++){
                            if(!parts[i].isBlank()){
                                firstValidIndex = i;
                                break;
                            }
                        }
                        if(ReadmeService.isSpecialRemark(parts[firstValidIndex])){
                            for (int i = firstValidIndex; i < parts.length; i++){
                                if (!parts[i].isBlank())
                                    specialAffectedHosp += String.format(COMMENT_FORMAT, parts[i]);
                            }
                        }
                    }
                    String ticketRelationships = genCommentedRelationships(promoForm);
                    String promoFormLink = promoForm.getK2FormLink().isBlank() ? "" : promoForm.getK2FormLink() + "&tab=PRD";
                    content.append(ppmAndTicket).append(specialAffectedHosp).append(ticketRelationships).append("\n").append(promoFormLink).append("\n\n");
                });
        return content.toString();
    }

    private static String genCommentedRelationships(PromoForm promoForm){
        if(!promoForm.getEndingTicketRelationshipMap().isEmpty()) {
            StringBuilder result = new StringBuilder();
            promoForm.getEndingTicketRelationshipMap().forEach((key, value) -> {
                String relationship = String.join(" and ", value);
                String comment = String.format(COMMENT_FORMAT, relationship + " " + key);
                result.append(comment);
            });
            return result.toString();
        }
        return "";
    }

    public static String compilePart2BackendScript(){
        StringBuilder content = new StringBuilder();
        DATA_CENTER.getKeyPromoFormMap().values().stream().filter(promoForm -> !promoForm.getImpManualItems().isEmpty()).forEach(promoForm -> {
            String ppmAndTicket = String.format(COMMENT_FORMAT, promoForm.getSummary() + "\t" + String.join(", ", promoForm.getAllTickets()));
            String impManualScript = String.join("\n", promoForm.getImpManualItems());
            content.append(ppmAndTicket).append("\n").append(impManualScript).append("\n\n");
        });
        DATA_CENTER.getKeyUrgentServiceFormMap().values().stream().filter(promoForm -> !promoForm.getImpManualItems().isEmpty()).sorted().forEach(promoForm -> {
            String ppmAndTicket = String.format(COMMENT_FORMAT, promoForm.getSummary() + "\t" + String.join(", ", promoForm.getAllTickets()));
            String impManualScript = String.join("\n", promoForm.getImpManualItems());
            content.append(ppmAndTicket).append("\n").append(impManualScript).append("\n\n");
        });
        return content.isEmpty() ? "None\n\n" : content.toString();
    }

    public static String compilePart3UrgentServicePromotion(){
        StringBuilder content = new StringBuilder();
        DATA_CENTER.getKeyUrgentServiceFormMap().values().stream().sorted().forEach(promoForm -> {
            String ppmAndTicket = String.format(COMMENT_FORMAT, promoForm.getSummary() + "\t" + String.join(", ", promoForm.getAllTickets()));
            String specialAffectedHosp = "";
            String affectedHosp = promoForm.getAffectedHosp();
            if (!affectedHosp.isBlank()){
                String[] parts = affectedHosp.split("\n");
                int firstValidIndex = 0;
                for (int i = 0; i < parts.length; i++){
                    if(!parts[i].isBlank()){
                        firstValidIndex = i;
                        break;
                    }
                }
                if(ReadmeService.isSpecialRemark(parts[firstValidIndex])){
                    for (int i = firstValidIndex; i < parts.length; i++){
                        if (!parts[i].isBlank())
                            specialAffectedHosp += String.format(COMMENT_FORMAT, parts[i]);
                    }
                }
            }
            String ticketRelationships = genCommentedRelationships(promoForm);
            String promoFormLink = promoForm.getK2FormLink().isBlank() ? "" : promoForm.getK2FormLink() + "&tab=PRD";
            content.append(ppmAndTicket).append(specialAffectedHosp).append(ticketRelationships).append("\n").append(promoFormLink).append("\n\n");
        });
        return content.isEmpty() ? "None\n\n" : content.toString();
    }
}
