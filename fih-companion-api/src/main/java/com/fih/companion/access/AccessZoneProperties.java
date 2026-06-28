package com.fih.companion.access;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@ConfigurationProperties(prefix = "fih")
public class AccessZoneProperties {

    private Map<Integer, List<String>> accessZones = new LinkedHashMap<>();

    public Map<Integer, List<String>> getAccessZones() {
        return accessZones;
    }

    public void setAccessZones(Map<Integer, List<String>> accessZones) {
        this.accessZones = accessZones;
    }
}
