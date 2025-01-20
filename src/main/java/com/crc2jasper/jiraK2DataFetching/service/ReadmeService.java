package com.crc2jasper.jiraK2DataFetching.service;

import com.crc2jasper.jiraK2DataFetching.component.DataCenter;
import com.crc2jasper.jiraK2DataFetching.component.PromoForm;
import com.crc2jasper.jiraK2DataFetching.config.SingletonConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ReadmeService {
    private ReadmeService(){}
    private static final DataCenter DATA_CENTER = DataCenter.getInstance();

    public static String genReadmeContent(){
        final int LONGEST_COL_WIDTH = getLongestColWidth();
        StringBuilder content = new StringBuilder(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "Promotion", "Remark"))
                .append("-".repeat(LONGEST_COL_WIDTH * 3)).append("\n");

        for (PromoForm promoForm: DATA_CENTER.getKeyPromoFormMap().values()){
            if (promoForm.getSummary().contains("PRN")) continue;
            String status = promoForm.getStatus();
            if(status.equalsIgnoreCase("Withdrawn")){
                status = "[WITHDRAWN] ";
            }else if (status.equalsIgnoreCase("Rejected")){
                status = "[REJECTED] ";
            }else status = "";
            Set<String> typeSet = new LinkedHashSet<>(promoForm.getTypes());
            final String finalTypeString = status + String.join(" & ", typeSet);
            content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", promoForm.getConcatenatedReadmePromoName(), finalTypeString));
            if (status.equalsIgnoreCase("[WITHDRAWN] ") || status.equalsIgnoreCase("[REJECTED] ")){
                content.append("\n");
                continue;
            }
            if(promoForm.isImpHospOrImpCorp()){
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
                    if(isSpecialRemark(parts[firstValidIndex])){
                        for (int i = firstValidIndex; i < parts.length; i++){
                            if (!parts[i].isBlank())
                                content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", parts[i]));
                        }
                    }
                    /*for(String part: parts){
                        if (!part.isBlank()) {
                            content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", part));
                        }
                    }*/
                }
                String concatenatedRelationshipString = concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString();
                if(!concatenatedRelationshipString.isBlank()){
                    content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", concatenatedRelationshipString));
                }
            }
            content.append("\n");
        }
        content.append("/").append("*".repeat(LONGEST_COL_WIDTH * 3)).append("/\n");
        List<PromoForm> allUrgentServiceForms = new ArrayList<>(DATA_CENTER.getKeyUrgentServiceFormMap().values());
        Collections.sort(allUrgentServiceForms);
        for (PromoForm promoForm: allUrgentServiceForms){
            String affectedHosp = promoForm.getAffectedHosp();
            if (!affectedHosp.isBlank()){
                String[] parts = affectedHosp.split("\n");
                content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", promoForm.getConcatenatedReadmePromoName(), parts[0]));
                if(parts.length > 1){
                    for(int i = 1; i < parts.length; i++){
                        content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", parts[i]));
                    }
                }
                String concatenatedRelationshipString = concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString();
                if(!concatenatedRelationshipString.isBlank()){
                    content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", concatenatedRelationshipString));
                }
            }else{
                content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", promoForm.getConcatenatedReadmePromoName(), concatenateTicketRelationships(promoForm).getConcatenatedRelationshipString()));
            }
            content.append("\n");
        }
        return content.toString();
    }

    public static boolean createReadmeFile(String content){
        Path path = Paths.get(DirectoryService.getTempSrcDirectory() + "\\Readme.txt");
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

    private static String abridgedSummary(String summary){
        try{
            return summary.split("_")[1];
        }catch (Exception e){
            return summary;
        }
    }

    private static int getLongestColWidth(){
        final int BUFFER = 20;
        int currLongest = 0;
        for (PromoForm promoForm: DATA_CENTER.getKeyPromoFormMap().values()){
            String summary = promoForm.getSummary();
            if(summary.contains("PRN")) continue;
            String concatenatedReadmePromoName = "";
            if(summary.contains("_")){
                concatenatedReadmePromoName = abridgedSummary(summary) + "_" + String.join(", ", promoForm.getAllTickets());
                promoForm.concatenatedReadmePromoName(concatenatedReadmePromoName);
            }else{
                String key_ITOCMS = promoForm.getKey_ITOCMS();
                concatenatedReadmePromoName = key_ITOCMS + "_" + String.join(", ", promoForm.getAllTickets());
                promoForm.concatenatedReadmePromoName(concatenatedReadmePromoName);
            }

            if(concatenatedReadmePromoName.length() > currLongest){
                currLongest = concatenatedReadmePromoName.length();
            }
        }
        for (PromoForm promoForm: DATA_CENTER.getKeyUrgentServiceFormMap().values()){
            String concatenatedReadmePromoName = abridgedSummary(promoForm.getSummary()) + "_" + String.join(", ", promoForm.getAllTickets());
            promoForm.concatenatedReadmePromoName(concatenatedReadmePromoName);
            if(concatenatedReadmePromoName.length() > currLongest){
                currLongest = concatenatedReadmePromoName.length();
            }
        }
        return currLongest + BUFFER;
    }

    private static PromoForm concatenateTicketRelationships(PromoForm promoForm){
        Map<String, Set<String>> endingTicketRelationship = promoForm.getEndingTicketRelationshipMap();
        List<String> relationshipStringList = new ArrayList<>();
        if(!endingTicketRelationship.isEmpty()){
            Map<String, String> endingTicketSummary = promoForm.getEndingTicketSummaryMap();
            for (Map.Entry<String, Set<String>> entry: endingTicketRelationship.entrySet()){
                String endingSummary = endingTicketSummary.get(entry.getKey());
                String endingTicket = entry.getKey() + (endingSummary.isBlank() ? "" : "_" + endingSummary);
                String relationshipString = String.join(" and ", entry.getValue());
                relationshipStringList.add(relationshipString + " " + endingTicket);
            }
            return promoForm.concatenatedRelationshipString(String.join("; ", relationshipStringList));
        }
        return promoForm.concatenatedRelationshipString("");
    }

    private static boolean isSpecialRemark(String statement){
        return SingletonConfig.getInstance().getHospList().stream().anyMatch(statement::contains);
    }
}
