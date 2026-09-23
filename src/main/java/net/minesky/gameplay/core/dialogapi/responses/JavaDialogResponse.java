package net.minesky.gameplay.core.dialogapi.responses;

import io.papermc.paper.dialog.DialogResponseView;
import net.minesky.gameplay.api.dialogapi.DialogResponse;
import org.bukkit.entity.Player;

public final class JavaDialogResponse implements DialogResponse {

    private final Player player;
    private final DialogResponseView view;

    public JavaDialogResponse(Player player, DialogResponseView view) {
        this.player = player;
        this.view = view;
    }

    @Override public Player getPlayer() { return player; }

    @Override
    public String getText(String key) {
        return view != null ? view.getText(key) : null;
    }

    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        if (view == null) return def;
        Boolean val = view.getBoolean(key);
        return val != null ? val : def;
    }

    @Override
    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    @Override
    public float getFloat(String key, float def) {
        if (view == null) return def;
        Float val = view.getFloat(key);
        return val != null ? val : def;
    }

    @Override
    public String getSelectedOption(String key) {
        return getText(key);
    }

    @Override
    public boolean has(String key) {
        return getText(key) != null;
    }
}