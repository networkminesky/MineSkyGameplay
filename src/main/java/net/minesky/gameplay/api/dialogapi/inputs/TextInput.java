package net.minesky.gameplay.api.dialogapi.inputs;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.api.dialogapi.DialogInputField;
import net.minesky.gameplay.core.dialogapi.DialogComponentUtils;

public final class TextInput implements DialogInputField<String> {

    private final String key;
    private final Component label;
    private final String defaultValue;
    private final String placeholder;
    private final int maxLength;

    private TextInput(Builder b) {
        this.key = b.key;
        this.label = b.label;
        this.defaultValue = b.defaultValue;
        this.placeholder = b.placeholder;
        this.maxLength = b.maxLength;
    }

    public static Builder builder(String key, Component label) { return new Builder(key, label); }
    public static Builder builder(String key, String label) { return new Builder(key, DialogComponentUtils.parse(label)); }

    @Override public String getKey() { return key; }
    @Override public Component getLabel() { return label; }
    @Override public String getDefaultValue() { return defaultValue; }
    public String getPlaceholder() { return placeholder; }
    public int getMaxLength() { return maxLength; }

    public static final class Builder {
        private final String key;
        private final Component label;
        private String defaultValue = "";
        private String placeholder = "";
        private int maxLength = 100;

        public Builder(String key, Component label) {
            this.key = key;
            this.label = label;
        }

        public Builder initial(String value) { this.defaultValue = value != null ? value : ""; return this; }
        public Builder placeholder(String placeholder) { this.placeholder = placeholder != null ? placeholder : ""; return this; }
        public Builder maxLength(int maxLength) { this.maxLength = maxLength; return this; }
        public TextInput build() { return new TextInput(this); }
    }
}