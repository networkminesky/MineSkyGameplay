package net.minesky.gameplay.core.dialogapi.responses;

import net.minesky.gameplay.api.dialogapi.DialogResponse;
import org.bukkit.entity.Player;

import java.util.Map;

public final class BedrockDialogResponse implements DialogResponse {

    private final Player player;
    private final Map<String, Object> values;

    public BedrockDialogResponse(Player player, Map<String, Object> values) {
        this.player = player;
        this.values = values;
    }

    @Override public Player getPlayer() { return player; }

    @Override
    public String getText(String key) {
        Object val = values.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        Object val = values.get(key);
        return val instanceof Boolean b ? b : def;
    }

    @Override
    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    @Override
    public float getFloat(String key, float def) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.floatValue();
        return def;
    }

    @Override
    public String getSelectedOption(String key) {
        return getText(key);
    }

    @Override
    public boolean has(String key) {
        return values.containsKey(key);
    }
}