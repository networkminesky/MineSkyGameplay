package net.minesky.gameplay.api.dialogapi.inputs;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.api.dialogapi.DialogInputField;
import net.minesky.gameplay.core.dialogapi.DialogComponentUtils;

public final class BoolInput implements DialogInputField<Boolean> {

    private final String key;
    private final Component label;
    private final boolean defaultValue;

    public BoolInput(String key, Component label, boolean defaultValue) {
        this.key = key;
        this.label = label;
        this.defaultValue = defaultValue;
    }

    public static BoolInput of(String key, String label, boolean defaultValue) {
        return new BoolInput(key, DialogComponentUtils.parse(label), defaultValue);
    }

    public static BoolInput of(String key, Component label, boolean defaultValue) {
        return new BoolInput(key, label, defaultValue);
    }

    @Override public String getKey() { return key; }
    @Override public Component getLabel() { return label; }
    @Override public Boolean getDefaultValue() { return defaultValue; }
}