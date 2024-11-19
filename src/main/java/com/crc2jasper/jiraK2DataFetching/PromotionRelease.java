package com.crc2jasper.jiraK2DataFetching;

public class PromotionRelease {
    private PromotionRelease(){}
    private final int maxNotificationCnt = 2;
    private final String standardReleaseName = "[Production]: CMS Normal Release";
    private final String releaseSender = "HA-CMS Non-Production Support Group";
    private boolean resendTmr = false;
    private String lastReleaseName = "";
    private String lastReleaseDate = "";
    private String year = "";
    private String batch = "";

    private static final PromotionRelease promotionRelease = new PromotionRelease();

    public static PromotionRelease getInstance(){return promotionRelease;}

    public PromotionRelease setLastReleaseName(String lastReleaseName){
        this.lastReleaseName = lastReleaseName;
        return this;
    }

    public String getLastReleaseName(){return this.lastReleaseName;}

    public PromotionRelease setLastReleaseDate(String lastReleaseDate){
        this.lastReleaseDate = lastReleaseDate;
        return this;
    }

    public String getLastReleaseDate(){return this.lastReleaseDate;}


    public PromotionRelease setYear(String year){
        this.year = year;
        return this;
    }

    public String getYear(){return this.year;}

    public PromotionRelease setBatch(String batch){
        this.batch = batch;
        return this;
    }

    public String getBatch(){return this.batch;}

    public boolean getResendTmrStatus(){return resendTmr;}

    public PromotionRelease resetResendTmrStatus(){
        this.resendTmr = false;
        return this;
    }

    public PromotionRelease setToResendTmr(){
        this.resendTmr = true;
        return this;
    }

    public boolean isValidPromotionRelease(String subject, String sender){
        return subject.contains(standardReleaseName) && sender.equalsIgnoreCase(releaseSender);
    }

    public boolean isDifferentRelease(String subject){
        return !this.lastReleaseName.equalsIgnoreCase(subject);
    }
}
