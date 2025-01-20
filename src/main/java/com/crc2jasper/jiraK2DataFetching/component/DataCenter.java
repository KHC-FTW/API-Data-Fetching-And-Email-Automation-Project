package com.crc2jasper.jiraK2DataFetching.component;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class DataCenter {
    private DataCenter(){}
    private final Map<String, PromoForm> keyPromoFormMap = new LinkedHashMap<>();
    private final Map<String, PromoForm> keyUrgentServiceFormMap = new LinkedHashMap<>();
    private static final DataCenter dataCenter = new DataCenter();
    public static DataCenter getInstance(){return dataCenter;}

    public void addPromoForm(String key_ITOCMS, PromoForm promoForm){
        keyPromoFormMap.put(key_ITOCMS, promoForm);
    }

    public void addUrgentServiceForm(String key_ITOCMS, PromoForm promoForm){
        keyUrgentServiceFormMap.put(key_ITOCMS, promoForm);
    }

    public PromoForm getPromoFormByKey_ITOCMS(String key_ITOCMS){
        return keyPromoFormMap.get(key_ITOCMS);
    }

    public PromoForm getUrgentServiceFormByKey_ITOCMS(String key_ITOCMS){
        return keyUrgentServiceFormMap.get(key_ITOCMS);
    }

    public void clearAllData(){
        keyPromoFormMap.clear();
        keyUrgentServiceFormMap.clear();
    }

}
