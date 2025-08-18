package org.openpnp.api.models.machine;

import java.util.List;

/**
 * DTO для информации о головке машины
 */
public class HeadInfo {
    private String id;
    private String name;
    private List<String> nozzleIds;
    private List<String> cameraIds;
    private List<String> actuatorIds;
    private double x;
    private double y;
    private double z;
    private double rotation;

    public HeadInfo() {
    }

    public HeadInfo(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getNozzleIds() {
        return nozzleIds;
    }

    public void setNozzleIds(List<String> nozzleIds) {
        this.nozzleIds = nozzleIds;
    }

    public List<String> getCameraIds() {
        return cameraIds;
    }

    public void setCameraIds(List<String> cameraIds) {
        this.cameraIds = cameraIds;
    }

    public List<String> getActuatorIds() {
        return actuatorIds;
    }

    public void setActuatorIds(List<String> actuatorIds) {
        this.actuatorIds = actuatorIds;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
}