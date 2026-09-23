package net.minesky.gameplay.api.dialogapi.inputs;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.api.dialogapi.DialogInputField;
import net.minesky.gameplay.api.dialogapi.DialogOption;
import net.minesky.gameplay.core.dialogapi.DialogComponentUtils;

import java.util.ArrayList;
import java.util.List;

public final class DropdownInput implements DialogInputField<String> {

    private final String key;
    private final Component label;
    private final List<DialogOption> options;
    private final int defaultIndex;

    private DropdownInput(Builder b) {
        this.key = b.key;
        this.label = b.label;
        this.options = List.copyOf(b.options);
        this.defaultIndex = b.defaultIndex;
    }

    public static Builder builder(String key, String label) {
        return new Builder(key, DialogComponentUtils.parse(label));
    }

    public static Builder builder(String key, Component label) {
        return new Builder(key, label);
    }

    @Override public String getKey() { return key; }
    @Override public Component getLabel() { return label; }
    @Override public String getDefaultValue() {
        return (!options.isEmpty() && defaultIndex >= 0 && defaultIndex < options.size())
                ? options.get(defaultIndex).id() : "";
    }
    public List<DialogOption> getOptions() { return options; }
    public int getDefaultIndex() { return defaultIndex; }

    public static final class Builder {
        private final String key;
        private final Component label;
        private final List<DialogOption> options = new ArrayList<>();
        private int defaultIndex = 0;

        public Builder(String key, Component label) {
            this.key = key;
            this.label = label;
        }

        public Builder option(String id, String label) {
            this.options.add(DialogOption.of(id, label));
            return this;
        }

        public Builder option(String id, Component label) {
            this.options.add(DialogOption.of(id, label));
            return this;
        }

        public Builder initial(String id) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).id().equalsIgnoreCase(id)) {
                    this.defaultIndex = i;
                    break;
                }
            }
            return this;
        }

        public Builder initial(int index) {
            this.defaultIndex = index;
            return this;
        }

        public DropdownInput build() { return new DropdownInput(this); }
    }
}