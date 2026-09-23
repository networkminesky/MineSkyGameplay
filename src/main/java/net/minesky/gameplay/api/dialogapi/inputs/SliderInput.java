package net.minesky.gameplay.api.dialogapi.inputs;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.api.dialogapi.DialogInputField;
import net.minesky.gameplay.core.dialogapi.DialogComponentUtils;

public final class SliderInput implements DialogInputField<Float> {

    private final String key;
    private final Component label;
    private final float min;
    private final float max;
    private final float step;
    private final float defaultValue;

    private SliderInput(Builder b) {
        this.key = b.key;
        this.label = b.label;
        this.min = b.min;
        this.max = b.max;
        this.step = b.step;
        this.defaultValue = b.defaultValue;
    }

    public static Builder builder(String key, String label, float min, float max) {
        return new Builder(key, DialogComponentUtils.parse(label), min, max);
    }

    public static Builder builder(String key, Component label, float min, float max) {
        return new Builder(key, label, min, max);
    }

    @Override public String getKey() { return key; }
    @Override public Component getLabel() { return label; }
    @Override public Float getDefaultValue() { return defaultValue; }
    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getStep() { return step; }

    public static final class Builder {
        private final String key;
        private final Component label;
        private final float min;
        private final float max;
        private float step = 1.0f;
        private float defaultValue;

        public Builder(String key, Component label, float min, float max) {
            this.key = key;
            this.label = label;
            this.min = min;
            this.max = max;
            this.defaultValue = min;
        }

        public Builder step(float step) { this.step = step; return this; }
        public Builder initial(float value) { this.defaultValue = value; return this; }
        public SliderInput build() { return new SliderInput(this); }
    }
}