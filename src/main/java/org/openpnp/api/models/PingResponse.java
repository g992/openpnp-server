package org.openpnp.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.openapi.OpenApiDescription;

/**
 * Ответ на ping запрос с информацией о статусе системы
 */
public class PingResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("version")
    private String version;

    @JsonProperty("uptime_ms")
    private long uptimeMs;

    @JsonProperty("machine_enabled")
    private Boolean machineEnabled;

    public PingResponse() {
    }

    public PingResponse(String status, String version, long uptimeMs, Boolean machineEnabled) {
        this.status = status;
        this.version = version;
        this.uptimeMs = uptimeMs;
        this.machineEnabled = machineEnabled;
    }

    // Геттеры и сеттеры
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getUptimeMs() {
        return uptimeMs;
    }

    public void setUptimeMs(long uptimeMs) {
        this.uptimeMs = uptimeMs;
    }

    public Boolean getMachineEnabled() {
        return machineEnabled;
    }

    public void setMachineEnabled(Boolean machineEnabled) {
        this.machineEnabled = machineEnabled;
    }
}