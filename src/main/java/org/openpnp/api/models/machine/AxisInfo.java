package org.openpnp.api.models.machine;

/**
 * DTO для информации об оси машины
 */
public class AxisInfo {
    private String id;
    private String name;
    private String type;
    private double position;
    private String unit;
    private boolean homed;
    private double homeCoordinate;

    public AxisInfo() {
    }

    public AxisInfo(String id, String name, String type, double position, String unit, boolean homed) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.position = position;
        this.unit = unit;
        this.homed = homed;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isHomed() {
        return homed;
    }

    public void setHomed(boolean homed) {
        this.homed = homed;
    }

    public double getHomeCoordinate() {
        return homeCoordinate;
    }

    public void setHomeCoordinate(double homeCoordinate) {
        this.homeCoordinate = homeCoordinate;
    }
}