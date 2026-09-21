package net.minesky.gameplay.features.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final boolean disableChat;

    public ChatListener(boolean disableChat) {
        this.disableChat = disableChat;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!disableChat || event.isCancelled()) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(
                Component.text("O chat está desligado por enquanto, aguarde.")
                        .color(NamedTextColor.RED)
        );
    }
}