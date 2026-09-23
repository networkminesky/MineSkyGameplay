package net.minesky.gameplay.api.dialogapi;

import net.kyori.adventure.text.Component;

public interface DialogInputField<T> {
    String getKey();
    Component getLabel();
    T getDefaultValue();
}