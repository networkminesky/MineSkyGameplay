package net.minesky.gameplay.core.dialogapi.responses;

import net.minesky.gameplay.api.dialogapi.DialogResponse;
import org.bukkit.entity.Player;

public final class EmptyDialogResponse implements DialogResponse {
    private final Player player;
    public EmptyDialogResponse(Player player) { this.player = player; }
    @Override public Player getPlayer() { return player; }
    @Override public String getText(String key) { return null; }
    @Override public boolean getBoolean(String key) { return false; }
    @Override public boolean getBoolean(String key, boolean def) { return def; }
    @Override public float getFloat(String key) { return 0; }
    @Override public float getFloat(String key, float def) { return def; }
    @Override public String getSelectedOption(String key) { return null; }
    @Override public boolean has(String key) { return false; }
}