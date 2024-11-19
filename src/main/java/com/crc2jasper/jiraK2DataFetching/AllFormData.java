package com.crc2jasper.jiraK2DataFetching;

import java.util.LinkedHashMap;
import java.util.Map;

public class AllFormData {

    private AllFormData(){}
    private static final AllFormData allFormData = new AllFormData();
    private Map<String, EmailForm> relatedEmailForms = new LinkedHashMap<>();
    private Map<String, EmailForm> unrelatedEmailForms = new LinkedHashMap<>();

    public static AllFormData getInstance(){return allFormData;}

    public void addRelatedEmailForm(String formNo, EmailForm emailForm){
        relatedEmailForms.put(formNo, emailForm);
    }

    public void removeRelatedEmailForm(String formNo){
        relatedEmailForms.remove(formNo);
    }

    public EmailForm getRelatedEmailForm(String formNo){
        return relatedEmailForms.get(formNo);
    }

    public Map<String, EmailForm> getAllRelatedEmailForms(){
        return relatedEmailForms;
    }

    //
    public void addUnrelatedEmailForm(String formNo, EmailForm emailForm){
        unrelatedEmailForms.put(formNo, emailForm);
    }

    public void removeUnrelatedEmailForm(String formNo){
        unrelatedEmailForms.remove(formNo);
    }

    public EmailForm getUnrelatedEmailForm(String formNo){
        return unrelatedEmailForms.get(formNo);
    }

    public Map<String, EmailForm> getAllUnrelatedEmailForms(){
        return unrelatedEmailForms;
    }

    public void clearAllEmailFormData(){
        relatedEmailForms.clear();
        unrelatedEmailForms.clear();
    }
}
