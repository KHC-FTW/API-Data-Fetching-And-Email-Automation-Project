package com.crc2jasper.jiraK2DataFetching;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TextSummary {
    private TextSummary(){}
    private static final TextSummary textSummary = new TextSummary();
    private final List<ReadmeItemPPM> allReadMeItemPPM = new ArrayList<>();
    private final List<ReadmeItemUrgentService> allReadmeItemUrgentServices = new ArrayList<>();
//    private static final PromotionRelease promotionRelease = PromotionRelease.getInstance();
    private static final CRInfo crInfo = CRInfo.getInstance();


    public static TextSummary getInstance(){return textSummary;}
    public void addReadmeItemPPM(ReadmeItemPPM readmeItemPPM){
        allReadMeItemPPM.add(readmeItemPPM);
    }

    public void addReadmeItemUrgentService(ReadmeItemUrgentService readmeItemUrgentService){
        allReadmeItemUrgentServices.add(readmeItemUrgentService);
    }
    public List<ReadmeItemPPM> getAllReadmeItemPPM(){
        return allReadMeItemPPM;
    }
    public List<ReadmeItemUrgentService> getAllReadmeItemUrgentServices(){
        return allReadmeItemUrgentServices;
    }

    public int getLongestColWidth(){
        final int BUFFER = 20;
        int currLongest = 0;
        for (ReadmeItemPPM item: allReadMeItemPPM){
            if (item.getPromotion().length() > currLongest){
                currLongest = item.getPromotion().length();
            }
        }
        for(ReadmeItemUrgentService item: allReadmeItemUrgentServices){
            if (item.getPromotion().length() > currLongest){
                currLongest = item.getPromotion().length();
            }
        }
        return currLongest + BUFFER;
    }

    public String genReadMeContent(){
        final int LONGEST_COL_WIDTH = getLongestColWidth();
        StringBuilder content = new StringBuilder(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "Promotion", "Remark"))
                .append("-".repeat(LONGEST_COL_WIDTH * 3)).append("\n");
//        String year_batch = promotionRelease.getYear() + "-" + promotionRelease.getBatch();
        for (ReadmeItemPPM item: allReadMeItemPPM){
            String status = item.getStatus();
            if(status.equalsIgnoreCase("Withdrawn")){
                status = "[WITHDRAWN] ";
            }else if (status.equalsIgnoreCase("Rejected")){
                status = "[REJECTED] ";
            }else status = "";
            String allTypes = item.getAllTypes();
            content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", item.getPromotion(), status + allTypes));
            if (allTypes.contains("imp-hosp") || allTypes.contains("imp-corp")){
                if (!item.getTargetHosp().isBlank() && !item.getTargetHosp().equalsIgnoreCase("N/A")){
                    String[] parts = item.getTargetHosp().split("\n");
                    for(String part: parts){
                        if (!part.isBlank()) {
                            content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", part));
                        }
                    }
                }
                String linkedIssues = item.getLinkedIssues();
                if (!linkedIssues.isBlank()){
                    content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", linkedIssues));
                }
//
//                if (!item.getCrInfo().isBlank()){
//                    String[] crParts = item.getCrInfo().split(";");
//                    for (String part: crParts){
//                        content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", part.strip()));
//                    }
//                }
            }
            content.append("\n");
        }
        content.append("/").append("*".repeat(LONGEST_COL_WIDTH * 3)).append("/\n");
        for (ReadmeItemUrgentService item: allReadmeItemUrgentServices){
            String allTypes = item.getAllTypes();
            content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", item.getPromotion(), allTypes));
            if (!item.getTargetHosp().isBlank() && !item.getTargetHosp().equalsIgnoreCase("N/A")){
                String[] parts = item.getTargetHosp().split("\n");
                for(String part: parts){
                    if (!part.isBlank()) {
                        content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", part));
                    }
                }
            }
            String linkedIssues = item.getLinkedIssues();
            if (!linkedIssues.isBlank()){
                content.append(String.format("%-" + LONGEST_COL_WIDTH + "s%s%n", "", linkedIssues));
            }
            content.append("\n");
        }
        return content.toString();
    }

    public void clearAllReadmeItems(){
        allReadMeItemPPM.clear();
    }

    public static boolean writeReadMeTxt(String content){
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

    @Deprecated
    public static boolean deleteReadMeTxt(){
        Path path = Paths.get(SingletonConfig.getIniInputPath() + "\\Readme.txt");
        if(Files.exists(path)){
            try {
                Files.delete(path);
                return true;
            } catch (Exception e) {
                System.out.println("Failed to delete Readme.txt file.\n");
                return false;
            }
        }
        return false;
    }

    public static String getAllCRTicketsFromDesc(String formSummary, String description) {
        /*  Example:

            VTS-257: To provide customization of preset vital signs routine order for newly admitted patients at CMS e-Vitals Maintenance page.
            EIO-14: Support edit e-I&O related frequency set up in CMS e-Vitals Maintenance page.
            EIO-15: Support edit e-I&O related order in CMS Vital Signs Order page.
            VTS-258: To provide visualization at CMS Vital Signs Order page for preset vital signs routine order.
            VTS-264: Improve responsive view of Vital Signs Order function in CMS 1920x1080 resolution
            VTS-265: Improve responsive view of eVitals Maintenance function in CMS 1920x1080 resolution

            ===== Please take note of below IssueLink information =====
            EIO-14 [clones] EIO-11
            EIO-15 [clones] EIO-12
            VTS-264 [clones] VTS-250
            VTS-265 [clones] VTS-251
        * */
        String[] lines = description.split("\n");
        List<String> crTickets = new ArrayList<>();
        List<String> crInfo = new ArrayList<>();
        final int TOTAL_RESULTS = 2;
        Map<String, String> allResults = new HashMap<>(TOTAL_RESULTS);
//        boolean hasCRInfo = false;
        for (String line : lines) {
            int colonIndex = line.indexOf(":");
            if (colonIndex != -1) {
                String crTicket = line.substring(0, colonIndex).trim();
                crTickets.add(crTicket);
            }
            /*if(!hasCRInfo && line.contains("=")){
                hasCRInfo = true;
                continue;
            }
            if (hasCRInfo && !line.isBlank()) {crInfo.add(line);}*/
        }
        /*
        * TODO:
        * May need to call jira api to fetch the issue details
        * */

        String parentCrTicket = crTickets.getFirst();
        List<String> allParentLinkedIssues = APIQueryService.fetchJiraCrTicketLinkedIssues(parentCrTicket);
        if(!allParentLinkedIssues.isEmpty()){
            CRInfo.compileFinalizedCrInfo(allParentLinkedIssues, formSummary);
        }

        return String.join(", ", crTickets);
//        allResults.put("crTickets", crTicketString);
//        if (!crInfo.isEmpty()){
//            allResults.put("crInfo", CRInfo.compileFinalizedCrInfo(crInfo));
//        }else{
//            allResults.put("crInfo", "");
//        }
//        return allResults;
    }

    public static String typeMapToSetString(Map<Integer, String> types){
        return String.join(" & ", new LinkedHashSet<>(types.values()));
    }

    public static String abridgedSummary(String summary){
        String[] parts = summary.split("_");
        if(parts.length >= 2) return parts[1];
        return "";
    }

}
