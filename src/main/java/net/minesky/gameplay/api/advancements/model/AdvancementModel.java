package net.minesky.gameplay.api.advancements.model;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public record AdvancementModel(
        String id,
        String categoryId,
        NamespacedKey namespacedKey,
        String title,
        String description,
        AdvancementFrame frame,
        int xpReward,
        ItemStack icon
) {}