package com.crc2jasper.jiraK2DataFetching;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailForm {
//    private String ppmType;
//    private String key;
    private String targetDate;
    private String affectedHosp;
    private String key;
    private String summary;
    private String description;
    private String promotionFormLink;
    private String promotionFormNo;
    private Map<Integer, String> types = new TreeMap<>();
    private String status;
    private boolean isToday;

    public EmailForm targetDate(String targetDate){
        this.targetDate = targetDate;
        return this;
    }

    public EmailForm affectedHosp(String affectedHosp){
        this.affectedHosp = affectedHosp;
        return this;
    }

    public EmailForm key(String key){
        this.key = key;
        return this;
    }

    public EmailForm summary(String summary){
        this.summary = summary;
        return this;
    }

    public EmailForm description(String description){
        this.description = description;
        return this;
    }

    public EmailForm promotionFormLink(String promotionFormLink){
        this.promotionFormLink = promotionFormLink;
        return this;
    }

    public EmailForm promotionFormNo(String promotionFormNo){
        this.promotionFormNo = promotionFormNo;
        return this;
    }

    public EmailForm addType(Integer sequenceNo, String type){
        this.types.put(sequenceNo, type);
        return this;
    }

    public EmailForm status(String status){
        this.status = status;
        return this;
    }

    public EmailForm isToday(boolean isToday){
        this.isToday = isToday;
        return this;
    }

    @Override
    public String toString(){
        return "Target Date: " + targetDate
               + "\nKey: " + key
               + "\nSummary: " + summary
               + "\nDescription: " + description
               + "\nPromotion Form Link: " + promotionFormLink
               + "\nPromotion Form No.: " + promotionFormNo
               + "\nType(s): " + types
               + "\nStatus: " + status;
    }

}
