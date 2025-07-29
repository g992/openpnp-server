package org.openpnp.api.models.machine;

import java.util.List;

/**
 * DTO для статуса машины
 */
public class MachineStatus {
    private boolean enabled;
    private boolean homed;
    private boolean busy;
    private String currentTask;
    private List<AxisInfo> axes;
    private List<HeadInfo> heads;
    private List<FeederInfo> feeders;
    private String motionPlannerType;

    public MachineStatus() {
    }

    public MachineStatus(boolean enabled, boolean homed, boolean busy, String currentTask) {
        this.enabled = enabled;
        this.homed = homed;
        this.busy = busy;
        this.currentTask = currentTask;
    }

    // Геттеры и сеттеры
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHomed() {
        return homed;
    }

    public void setHomed(boolean homed) {
        this.homed = homed;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public String getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(String currentTask) {
        this.currentTask = currentTask;
    }

    public List<AxisInfo> getAxes() {
        return axes;
    }

    public void setAxes(List<AxisInfo> axes) {
        this.axes = axes;
    }

    public List<HeadInfo> getHeads() {
        return heads;
    }

    public void setHeads(List<HeadInfo> heads) {
        this.heads = heads;
    }

    public List<FeederInfo> getFeeders() {
        return feeders;
    }

    public void setFeeders(List<FeederInfo> feeders) {
        this.feeders = feeders;
    }

    public String getMotionPlannerType() {
        return motionPlannerType;
    }

    public void setMotionPlannerType(String motionPlannerType) {
        this.motionPlannerType = motionPlannerType;
    }
}