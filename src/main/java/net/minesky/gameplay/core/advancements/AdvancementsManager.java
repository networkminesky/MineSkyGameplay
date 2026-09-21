package net.minesky.gameplay.core.advancements;

import net.minesky.gameplay.MineSkyGameplayPlugin;
import net.minesky.gameplay.api.advancements.AdvancementsAPI;
import net.minesky.gameplay.api.advancements.model.AdvancementModel;
import net.minesky.gameplay.api.advancements.model.CategoryModel;
import net.minesky.gameplay.core.advancements.menu.BedrockMenuManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancementsManager implements AdvancementsAPI {

    private final MineSkyGameplayPlugin plugin;
    private final BedrockMenuManager menuManager;

    private final Map<NamespacedKey, AdvancementModel> advancementsByKey = new ConcurrentHashMap<>();
    private final Map<String, NamespacedKey> aliasMap = new ConcurrentHashMap<>();
    private final Map<String, CategoryModel> categories = new LinkedHashMap<>();

    public AdvancementsManager(MineSkyGameplayPlugin plugin) {
        this.plugin = plugin;
        this.menuManager = new BedrockMenuManager(plugin);
    }

    public void start() {
        Bukkit.getServicesManager().register(AdvancementsAPI.class, this, plugin, ServicePriority.Normal);
    }

    public void stop() {
        Bukkit.getServicesManager().unregister(AdvancementsAPI.class, this);
        clearRegistry();
    }

    public synchronized void clearRegistry() {
        advancementsByKey.clear();
        aliasMap.clear();
        categories.clear();
    }

    @Override
    public void registerCategory(CategoryModel category) {
        categories.put(category.id().toLowerCase(), category);
    }

    @Override
    public CategoryModel getCategory(String id) {
        return categories.get(id.toLowerCase());
    }

    @Override
    public Collection<CategoryModel> getAllCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    @Override
    public void registerAdvancement(AdvancementModel model) {
        NamespacedKey key = model.namespacedKey();
        advancementsByKey.put(key, model);
        aliasMap.put(model.id().toLowerCase(), key);
        aliasMap.put((model.categoryId() + "/" + model.id()).toLowerCase(), key);
        aliasMap.put(key.toString().toLowerCase(), key);
    }

    @Override
    public CompletableFuture<Boolean> grantAsync(Player player, String keyOrId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (player == null || !player.isOnline()) {
            future.complete(false);
            return future;
        }

        NamespacedKey key = resolveKey(keyOrId);
        if (key == null) {
            future.complete(false);
            return future;
        }

        player.getScheduler().run(plugin, task -> future.complete(grantInternal(player, key)), null);
        return future;
    }

    @Override
    public boolean grant(Player player, String keyOrId) {
        if (player == null || !player.isOnline()) return false;
        NamespacedKey key = resolveKey(keyOrId);
        if (key == null) return false;

        if (Bukkit.isOwnedByCurrentRegion(player)) {
            return grantInternal(player, key);
        } else {
            grantAsync(player, keyOrId);
            return true;
        }
    }

    private boolean grantInternal(Player player, NamespacedKey key) {
        Advancement adv = Bukkit.getAdvancement(key);
        if (adv == null) return false;

        AdvancementProgress progress = player.getAdvancementProgress(adv);
        if (progress.isDone()) return false;

        for (String criteria : progress.getRemainingCriteria()) {
            progress.awardCriteria(criteria);
        }
        return true;
    }

    @Override
    public boolean hasAdvancement(Player player, String keyOrId) {
        if (player == null || !player.isOnline()) return false;
        NamespacedKey key = resolveKey(keyOrId);
        if (key == null) return false;

        Advancement adv = Bukkit.getAdvancement(key);
        if (adv == null) return false;

        return player.getAdvancementProgress(adv).isDone();
    }

    @Override
    public int getCategoryCompletedCount(Player player, String categoryId) {
        int count = 0;
        for (AdvancementModel model : advancementsByKey.values()) {
            if (model.categoryId().equalsIgnoreCase(categoryId) && hasAdvancement(player, model.id())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getTotalCompletedCount(Player player) {
        int count = 0;
        for (AdvancementModel model : advancementsByKey.values()) {
            if (hasAdvancement(player, model.id())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void openBedrockMenu(Player player) {
        menuManager.openMainMenu(player);
    }

    @Override
    public NamespacedKey resolveKey(String query) {
        if (query == null) return null;
        return aliasMap.get(query.toLowerCase());
    }

    @Override
    public Collection<AdvancementModel> getAllAdvancements() {
        return Collections.unmodifiableCollection(advancementsByKey.values());
    }

    @Override
    public List<AdvancementModel> getByCategory(String categoryId) {
        List<AdvancementModel> list = new ArrayList<>();
        for (AdvancementModel model : advancementsByKey.values()) {
            if (model.categoryId().equalsIgnoreCase(categoryId)) {
                list.add(model);
            }
        }
        return list;
    }

    public BedrockMenuManager getMenuManager() {
        return menuManager;
    }
}