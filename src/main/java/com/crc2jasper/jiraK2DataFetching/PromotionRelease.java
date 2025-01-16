package com.crc2jasper.jiraK2DataFetching;

public class PromotionRelease {
    private PromotionRelease(){}
    private final String STANDARD_RELEASE_NAME = "[Production]: CMS Normal Release";
    private final String RELEASE_SENDER = "HA-CMS Non-Production Support Group";
    private boolean resendTmr = false;
    private String lastReleaseName = "";
    private String lastReleaseDate = "";
    private String year = "";
    private String batch = "";

    private static final PromotionRelease PROMOTION_RELEASE = new PromotionRelease();

    public static PromotionRelease getInstance(){return PROMOTION_RELEASE;}

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
        return subject.contains(STANDARD_RELEASE_NAME) && sender.equalsIgnoreCase(RELEASE_SENDER);
    }

    public boolean isDifferentRelease(String subject){
        return !this.lastReleaseName.equalsIgnoreCase(subject);
    }
}
