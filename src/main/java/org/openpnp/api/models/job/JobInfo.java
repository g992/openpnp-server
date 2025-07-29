package org.openpnp.api.models.job;

import java.util.List;

/**
 * DTO для информации о задании
 */
public class JobInfo {
    private String name;
    private String file;
    private boolean dirty;
    private List<PlacementInfo> placements;
    private int totalPlacements;
    private int completedPlacements;
    private String status;

    public JobInfo() {
    }

    public JobInfo(String name, String file, boolean dirty) {
        this.name = name;
        this.file = file;
        this.dirty = dirty;
    }

    // Геттеры и сеттеры
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public List<PlacementInfo> getPlacements() {
        return placements;
    }

    public void setPlacements(List<PlacementInfo> placements) {
        this.placements = placements;
    }

    public int getTotalPlacements() {
        return totalPlacements;
    }

    public void setTotalPlacements(int totalPlacements) {
        this.totalPlacements = totalPlacements;
    }

    public int getCompletedPlacements() {
        return completedPlacements;
    }

    public void setCompletedPlacements(int completedPlacements) {
        this.completedPlacements = completedPlacements;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}