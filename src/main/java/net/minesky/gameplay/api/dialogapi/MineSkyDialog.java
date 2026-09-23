package net.minesky.gameplay.api.dialogapi;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.api.dialogapi.inputs.BoolInput;
import net.minesky.gameplay.api.dialogapi.inputs.DropdownInput;
import net.minesky.gameplay.api.dialogapi.inputs.SliderInput;
import net.minesky.gameplay.api.dialogapi.inputs.TextInput;
import net.minesky.gameplay.core.dialogapi.DialogComponentUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MineSkyDialog {

    private final Component title;
    private final List<Component> body;
    private final List<ItemStack> items;
    private final List<DialogInputField<?>> inputs;
    private final List<DialogButton> buttons;
    private final boolean canCloseWithEscape;
    private final @Nullable BiConsumer<Player, DialogResponse> submitAction;
    private final @Nullable Consumer<Player> closeAction;

    private MineSkyDialog(Builder builder) {
        this.title = builder.title;
        this.body = List.copyOf(builder.body);
        this.items = List.copyOf(builder.items);
        this.inputs = List.copyOf(builder.inputs);
        this.buttons = List.copyOf(builder.buttons);
        this.canCloseWithEscape = builder.canCloseWithEscape;
        this.submitAction = builder.submitAction;
        this.closeAction = builder.closeAction;
    }

    public static Builder builder() { return new Builder(); }
    public static Builder builder(Component title) { return new Builder().title(title); }
    public static Builder builder(String title) { return new Builder().title(title); }

    public void show(Player player) {
        DialogAPI api = DialogAPI.get();
        if (api != null) {
            api.show(player, this);
        }
    }

    public Component getTitle() { return title; }
    public List<Component> getBody() { return body; }
    public List<ItemStack> getItems() { return items; }
    public List<DialogInputField<?>> getInputs() { return inputs; }
    public List<DialogButton> getButtons() { return buttons; }
    public boolean isCanCloseWithEscape() { return canCloseWithEscape; }
    public @Nullable BiConsumer<Player, DialogResponse> getSubmitAction() { return submitAction; }
    public @Nullable Consumer<Player> getCloseAction() { return closeAction; }
    public boolean hasInputs() { return !inputs.isEmpty(); }

    public static final class Builder {
        private Component title = Component.text("Menu");
        private final List<Component> body = new ArrayList<>();
        private final List<ItemStack> items = new ArrayList<>();
        private final List<DialogInputField<?>> inputs = new ArrayList<>();
        private final List<DialogButton> buttons = new ArrayList<>();
        private boolean canCloseWithEscape = true;
        private BiConsumer<Player, DialogResponse> submitAction;
        private Consumer<Player> closeAction;

        public Builder title(Component title) {
            this.title = title;
            return this;
        }

        public Builder title(String title) {
            this.title = DialogComponentUtils.parse(title);
            return this;
        }

        public Builder body(Component component) {
            this.body.add(component);
            return this;
        }

        public Builder body(String text) {
            this.body.add(DialogComponentUtils.parse(text));
            return this;
        }

        public Builder bodyItem(ItemStack item) {
            if (item != null) this.items.add(item);
            return this;
        }

        public Builder canCloseWithEscape(boolean escape) {
            this.canCloseWithEscape = escape;
            return this;
        }

        public Builder addInput(DialogInputField<?> field) {
            this.inputs.add(field);
            return this;
        }

        public Builder textInput(String key, String label, String placeholder, String initial) {
            return addInput(TextInput.builder(key, label).placeholder(placeholder).initial(initial).build());
        }

        public Builder boolInput(String key, String label, boolean initial) {
            return addInput(BoolInput.of(key, label, initial));
        }

        public Builder sliderInput(String key, String label, float min, float max, float step, float initial) {
            return addInput(SliderInput.builder(key, label, min, max).step(step).initial(initial).build());
        }

        public Builder dropdownInput(String key, String label, Consumer<DropdownInput.Builder> consumer) {
            DropdownInput.Builder b = DropdownInput.builder(key, label);
            consumer.accept(b);
            return addInput(b.build());
        }

        public Builder button(DialogButton button) {
            this.buttons.add(button);
            return this;
        }

        public Builder button(String label, Consumer<Player> onClick) {
            return button(DialogButton.of(label, onClick));
        }

        public Builder button(Component label, Consumer<Player> onClick) {
            return button(DialogButton.of(label, onClick));
        }

        public Builder button(String label, BiConsumer<Player, DialogResponse> onClick) {
            return button(DialogButton.of(label, onClick));
        }

        public Builder confirmButton(String label, Consumer<Player> onClick) {
            return button(DialogButton.confirm(label, onClick));
        }

        public Builder cancelButton(String label, Consumer<Player> onClick) {
            return button(DialogButton.cancel(label, onClick));
        }

        public Builder onSubmit(BiConsumer<Player, DialogResponse> submitAction) {
            this.submitAction = submitAction;
            return this;
        }

        public Builder onClose(Consumer<Player> closeAction) {
            this.closeAction = closeAction;
            return this;
        }

        public MineSkyDialog build() {
            return new MineSkyDialog(this);
        }

        public void show(Player player) {
            build().show(player);
        }
    }
}