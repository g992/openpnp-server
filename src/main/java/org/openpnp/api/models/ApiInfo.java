package org.openpnp.api.models;

import java.util.Map;

/**
 * Общая информация об API
 */
public class ApiInfo {
    private String name;
    private String description;
    private String version;
    private Map<String, String> endpoints;

    public ApiInfo() {
    }

    public ApiInfo(String name, String description, String version, Map<String, String> endpoints) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.endpoints = endpoints;
    }

    // Геттеры и сеттеры
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, String> endpoints) {
        this.endpoints = endpoints;
    }
}