package net.minesky.gameplay.core.dialogapi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class DialogComponentUtils {

    private DialogComponentUtils() {}

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        if (input.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
        if (input.contains("§")) {
            return LegacyComponentSerializer.legacySection().deserialize(input);
        }
        try {
            return MiniMessage.miniMessage().deserialize(input);
        } catch (Exception e) {
            return Component.text(input);
        }
    }

    public static String toBedrock(Component component) {
        if (component == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}