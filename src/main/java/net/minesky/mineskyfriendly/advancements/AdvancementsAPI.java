package net.minesky.mineskyfriendly.advancements;

import net.minesky.mineskyfriendly.MineSkyFriendly;
import net.minesky.mineskyfriendly.advancements.data.AdvancementModel;
import net.minesky.mineskyfriendly.advancements.data.CategoryModel;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancementsAPI {

    private static AdvancementsAPI instance;
    private final MineSkyFriendly plugin;

    private final Map<NamespacedKey, AdvancementModel> advancementsByKey = new ConcurrentHashMap<>();
    private final Map<String, NamespacedKey> aliasMap = new ConcurrentHashMap<>();
    private final Map<String, CategoryModel> categories = new LinkedHashMap<>();

    public AdvancementsAPI(MineSkyFriendly plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static AdvancementsAPI get() {
        if (instance == null) {
            throw new IllegalStateException("AdvancementsAPI não foi inicializada!");
        }
        return instance;
    }

    public synchronized void clearRegistry() {
        advancementsByKey.clear();
        aliasMap.clear();
        categories.clear();
    }

    public void registerCategory(CategoryModel category) {
        categories.put(category.id().toLowerCase(), category);
    }

    public CategoryModel getCategory(String id) {
        return categories.get(id.toLowerCase());
    }

    public Collection<CategoryModel> getAllCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    public void registerAdvancement(AdvancementModel model) {
        NamespacedKey key = model.namespacedKey();
        advancementsByKey.put(key, model);
        aliasMap.put(model.id().toLowerCase(), key);
        aliasMap.put((model.categoryId() + "/" + model.id()).toLowerCase(), key);
        aliasMap.put(key.toString().toLowerCase(), key);
    }

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

    public boolean hasAdvancement(Player player, String keyOrId) {
        if (player == null || !player.isOnline()) return false;
        NamespacedKey key = resolveKey(keyOrId);
        if (key == null) return false;

        Advancement adv = Bukkit.getAdvancement(key);
        if (adv == null) return false;

        return player.getAdvancementProgress(adv).isDone();
    }

    public int getCategoryCompletedCount(Player player, String categoryId) {
        int count = 0;
        for (AdvancementModel model : advancementsByKey.values()) {
            if (model.categoryId().equalsIgnoreCase(categoryId) && hasAdvancement(player, model.id())) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCompletedCount(Player player) {
        int count = 0;
        for (AdvancementModel model : advancementsByKey.values()) {
            if (hasAdvancement(player, model.id())) {
                count++;
            }
        }
        return count;
    }

    public boolean isBedrockPlayer(Player player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    public void openBedrockMenu(Player player) {
        plugin.getMenuManager().openMainMenu(player);
    }

    public NamespacedKey resolveKey(String query) {
        if (query == null) return null;
        return aliasMap.get(query.toLowerCase());
    }

    public Collection<AdvancementModel> getAllAdvancements() {
        return Collections.unmodifiableCollection(advancementsByKey.values());
    }

    public List<AdvancementModel> getByCategory(String categoryId) {
        List<AdvancementModel> list = new ArrayList<>();
        for (AdvancementModel model : advancementsByKey.values()) {
            if (model.categoryId().equalsIgnoreCase(categoryId)) {
                list.add(model);
            }
        }
        return list;
    }
}