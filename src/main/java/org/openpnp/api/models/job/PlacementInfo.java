package org.openpnp.api.models.job;

/**
 * DTO для информации о размещении компонента
 */
public class PlacementInfo {
    public enum Type {
        PLACE,
        FIDUCIAL,
        IGNORE
    }

    public enum Side {
        TOP,
        BOTTOM
    }

    private String id;
    private String boardName;
    private String partId;
    private String packageId;
    private Type type;
    private Side side;
    private double x;
    private double y;
    private double z;
    private double rotation;
    private boolean enabled;
    private boolean placed;
    private String comment;

    public PlacementInfo() {
    }

    public PlacementInfo(String id, String partId, Type type, Side side, double x, double y, double rotation) {
        this.id = id;
        this.partId = partId;
        this.type = type;
        this.side = side;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.enabled = true;
        this.placed = false;
    }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public String getPartId() {
        return partId;
    }

    public void setPartId(String partId) {
        this.partId = partId;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPlaced() {
        return placed;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}