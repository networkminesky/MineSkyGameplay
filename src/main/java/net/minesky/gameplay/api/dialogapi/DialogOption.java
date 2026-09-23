package net.minesky.gameplay.api.dialogapi;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.core.dialogapi.DialogComponentUtils;

public record DialogOption(String id, Component label) {
    public static DialogOption of(String id, Component label) {
        return new DialogOption(id, label);
    }
    public static DialogOption of(String id, String label) {
        return new DialogOption(id, DialogComponentUtils.parse(label));
    }
}