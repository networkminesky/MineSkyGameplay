package net.minesky.mineskyfriendly.advancements.data;

public enum AdvancementFrame {
    TASK("Tarefa", "§a"),
    GOAL("Meta", "§e"),
    CHALLENGE("Desafio", "§d");

    private final String displayName;
    private final String color;

    AdvancementFrame(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }
}