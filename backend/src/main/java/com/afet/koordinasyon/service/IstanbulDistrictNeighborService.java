package com.afet.koordinasyon.service;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Istanbul 39 district neighbor map for proximity-based scoring.
 * Neighbors are defined as geographically adjacent districts sharing a border.
 */
@Component
public class IstanbulDistrictNeighborService {

    private static final Map<String, Set<String>> NEIGHBORS;

    static {
        Map<String, Set<String>> m = new HashMap<>();

        m.put("Adalar",         Set.of("Maltepe", "Pendik"));
        m.put("Arnavutköy",     Set.of("Eyüpsultan", "Başakşehir", "Büyükçekmece", "Çatalca"));
        m.put("Ataşehir",       Set.of("Kadıköy", "Ümraniye", "Maltepe", "Sancaktepe", "Üsküdar"));
        m.put("Avcılar",        Set.of("Esenyurt", "Küçükçekmece", "Beylikdüzü", "Bağcılar"));
        m.put("Bağcılar",       Set.of("Avcılar", "Bahçelievler", "Güngören", "Esenler", "Küçükçekmece", "Başakşehir"));
        m.put("Bahçelievler",   Set.of("Bağcılar", "Bakırköy", "Güngören", "Zeytinburnu"));
        m.put("Bakırköy",       Set.of("Bahçelievler", "Zeytinburnu", "Küçükçekmece"));
        m.put("Başakşehir",     Set.of("Bağcılar", "Arnavutköy", "Esenler", "Sultangazi", "Küçükçekmece"));
        m.put("Bayrampaşa",     Set.of("Esenler", "Gaziosmanpaşa", "Güngören", "Kağıthane", "Eyüpsultan", "Fatih"));
        m.put("Beşiktaş",       Set.of("Şişli", "Sarıyer", "Kağıthane", "Beyoğlu"));
        m.put("Beykoz",         Set.of("Sarıyer", "Şile", "Çekmeköy", "Üsküdar", "Ümraniye"));
        m.put("Beylikdüzü",     Set.of("Esenyurt", "Avcılar", "Büyükçekmece"));
        m.put("Beyoğlu",        Set.of("Beşiktaş", "Şişli", "Kağıthane", "Fatih", "Eyüpsultan"));
        m.put("Büyükçekmece",   Set.of("Arnavutköy", "Beylikdüzü", "Esenyurt", "Çatalca", "Silivri"));
        m.put("Çatalca",        Set.of("Arnavutköy", "Büyükçekmece", "Silivri"));
        m.put("Çekmeköy",       Set.of("Beykoz", "Şile", "Ümraniye", "Sancaktepe"));
        m.put("Esenler",        Set.of("Bağcılar", "Başakşehir", "Bayrampaşa", "Güngören", "Gaziosmanpaşa"));
        m.put("Esenyurt",       Set.of("Avcılar", "Beylikdüzü", "Büyükçekmece", "Bağcılar", "Küçükçekmece"));
        m.put("Eyüpsultan",     Set.of("Arnavutköy", "Bayrampaşa", "Beyoğlu", "Gaziosmanpaşa", "Kağıthane", "Sultangazi"));
        m.put("Fatih",          Set.of("Beyoğlu", "Bayrampaşa", "Güngören", "Zeytinburnu", "Eminönü"));
        m.put("Gaziosmanpaşa",  Set.of("Bayrampaşa", "Esenler", "Eyüpsultan", "Sultangazi", "Kağıthane"));
        m.put("Güngören",       Set.of("Bağcılar", "Bahçelievler", "Bayrampaşa", "Esenler", "Zeytinburnu"));
        m.put("Kadıköy",        Set.of("Ataşehir", "Maltepe", "Üsküdar"));
        m.put("Kağıthane",      Set.of("Beşiktaş", "Beyoğlu", "Eyüpsultan", "Gaziosmanpaşa", "Sarıyer", "Şişli"));
        m.put("Kartal",         Set.of("Maltepe", "Pendik", "Sancaktepe"));
        m.put("Küçükçekmece",   Set.of("Avcılar", "Bağcılar", "Bakırköy", "Başakşehir", "Esenyurt"));
        m.put("Maltepe",        Set.of("Ataşehir", "Kartal", "Kadıköy", "Adalar"));
        m.put("Pendik",         Set.of("Kartal", "Sancaktepe", "Sultanbeyli", "Tuzla", "Adalar"));
        m.put("Sancaktepe",     Set.of("Ataşehir", "Çekmeköy", "Kartal", "Pendik", "Sultanbeyli", "Ümraniye"));
        m.put("Sarıyer",        Set.of("Beşiktaş", "Beykoz", "Kağıthane", "Eyüpsultan"));
        m.put("Silivri",        Set.of("Çatalca", "Büyükçekmece"));
        m.put("Sultanbeyli",    Set.of("Pendik", "Sancaktepe", "Ümraniye"));
        m.put("Sultangazi",     Set.of("Başakşehir", "Eyüpsultan", "Gaziosmanpaşa"));
        m.put("Şile",           Set.of("Beykoz", "Çekmeköy"));
        m.put("Şişli",          Set.of("Beşiktaş", "Beyoğlu", "Kağıthane", "Sarıyer", "Eyüpsultan"));
        m.put("Tuzla",          Set.of("Pendik"));
        m.put("Ümraniye",       Set.of("Ataşehir", "Beykoz", "Çekmeköy", "Sancaktepe", "Sultanbeyli", "Üsküdar"));
        m.put("Üsküdar",        Set.of("Beykoz", "Kadıköy", "Ümraniye", "Ataşehir"));
        m.put("Zeytinburnu",    Set.of("Bahçelievler", "Bakırköy", "Fatih", "Güngören"));

        NEIGHBORS = Collections.unmodifiableMap(m);
    }

    public Set<String> getNeighbors(String districtName) {
        if (districtName == null) return Set.of();
        return NEIGHBORS.getOrDefault(districtName, Set.of());
    }

    public boolean areNeighbors(String d1, String d2) {
        return getNeighbors(d1).contains(d2) || getNeighbors(d2).contains(d1);
    }
}
