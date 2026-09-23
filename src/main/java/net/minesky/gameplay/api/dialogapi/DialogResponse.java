package net.minesky.gameplay.api.dialogapi;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface DialogResponse {

    Player getPlayer();

    @Nullable
    String getText(String key);

    default String getText(String key, String def) {
        String val = getText(key);
        return val != null ? val : def;
    }

    boolean getBoolean(String key);

    boolean getBoolean(String key, boolean def);

    float getFloat(String key);

    float getFloat(String key, float def);

    default int getInt(String key) {
        return Math.round(getFloat(key));
    }

    default int getInt(String key, int def) {
        return Math.round(getFloat(key, (float) def));
    }

    @Nullable
    String getSelectedOption(String key);

    default String getSelectedOption(String key, String def) {
        String val = getSelectedOption(key);
        return val != null ? val : def;
    }

    boolean has(String key);
}