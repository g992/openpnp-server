package org.openpnp.api.models;

/**
 * Информация о системе для ping endpoint
 */
public class SystemInfo {
    private String version;
    private long uptimeMs;
    private boolean machineEnabled;
    private String timestamp;

    public SystemInfo() {
    }

    public SystemInfo(String version, long uptimeMs, boolean machineEnabled, String timestamp) {
        this.version = version;
        this.uptimeMs = uptimeMs;
        this.machineEnabled = machineEnabled;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры
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

    public boolean isMachineEnabled() {
        return machineEnabled;
    }

    public void setMachineEnabled(boolean machineEnabled) {
        this.machineEnabled = machineEnabled;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}