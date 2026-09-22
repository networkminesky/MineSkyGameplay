package net.minesky.gameplay.features.homes;

public class SavedLocationEntry {

    public enum Type {
        HOME,
        CLAIM,
        CLAN
    }

    private final Type type;
    private final String name;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final int area;

    public SavedLocationEntry(Type type, String name, String world, int x, int y, int z, int area) {
        this.type = type;
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.area = area;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getArea() {
        return area;
    }
}