package net.minesky.gameplay.features.homes;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class HomesMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private final int page;
    private final String filter;
    private final List<SavedLocationEntry> entries;
    private final UUID targetUUID;
    private final String targetName;

    public HomesMenuHolder(int page, String filter, List<SavedLocationEntry> entries, UUID targetUUID, String targetName) {
        this.page = page;
        this.filter = filter;
        this.entries = entries;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
    }

    public int getPage() {
        return page;
    }

    public String getFilter() {
        return filter;
    }

    public List<SavedLocationEntry> getEntries() {
        return entries;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public String getTargetName() {
        return targetName;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}