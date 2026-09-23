package net.minesky.gameplay.api.dialogapi;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.core.dialogapi.DialogComponentUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class DialogButton {

    private final Component label;
    private final @Nullable Component tooltip;
    private final int width;
    private final @Nullable String iconUrl;
    private final @Nullable String iconPath;
    private final BiConsumer<Player, DialogResponse> action;

    private DialogButton(Builder builder) {
        this.label = builder.label;
        this.tooltip = builder.tooltip;
        this.width = builder.width;
        this.iconUrl = builder.iconUrl;
        this.iconPath = builder.iconPath;
        this.action = builder.action;
    }

    public static DialogButton of(Component label, Consumer<Player> onClick) {
        return builder().label(label).onClick(onClick).build();
    }

    public static DialogButton of(String label, Consumer<Player> onClick) {
        return builder().label(label).onClick(onClick).build();
    }

    public static DialogButton of(Component label, BiConsumer<Player, DialogResponse> onClick) {
        return builder().label(label).onClick(onClick).build();
    }

    public static DialogButton of(String label, BiConsumer<Player, DialogResponse> onClick) {
        return builder().label(label).onClick(onClick).build();
    }

    public static DialogButton confirm(String label, Consumer<Player> onClick) {
        return builder().label(label).width(150).onClick(onClick).build();
    }

    public static DialogButton confirm(String label, BiConsumer<Player, DialogResponse> onClick) {
        return builder().label(label).width(150).onClick(onClick).build();
    }

    public static DialogButton cancel(String label, Consumer<Player> onClick) {
        return builder().label(label).width(150).onClick(onClick).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Component getLabel() { return label; }
    public @Nullable Component getTooltip() { return tooltip; }
    public int getWidth() { return width; }
    public @Nullable String getIconUrl() { return iconUrl; }
    public @Nullable String getIconPath() { return iconPath; }
    public BiConsumer<Player, DialogResponse> getAction() { return action; }

    public void click(Player player, DialogResponse response) {
        if (action != null) {
            action.accept(player, response);
        }
    }

    public static final class Builder {
        private Component label = Component.text("OK");
        private Component tooltip;
        private int width = 150;
        private String iconUrl;
        private String iconPath;
        private BiConsumer<Player, DialogResponse> action = (p, r) -> {};

        public Builder label(Component label) {
            this.label = label;
            return this;
        }

        public Builder label(String label) {
            this.label = DialogComponentUtils.parse(label);
            return this;
        }

        public Builder tooltip(Component tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder tooltip(String tooltip) {
            this.tooltip = DialogComponentUtils.parse(tooltip);
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder iconUrl(String url) {
            this.iconUrl = url;
            return this;
        }

        public Builder iconPath(String path) {
            this.iconPath = path;
            return this;
        }

        public Builder onClick(Consumer<Player> action) {
            this.action = (player, res) -> action.accept(player);
            return this;
        }

        public Builder onClick(BiConsumer<Player, DialogResponse> action) {
            this.action = action;
            return this;
        }

        public DialogButton build() {
            return new DialogButton(this);
        }
    }
}