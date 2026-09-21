package net.minesky.gameplay.api.advancements;

import net.minesky.gameplay.api.advancements.model.AdvancementModel;
import net.minesky.gameplay.api.advancements.model.CategoryModel;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AdvancementsAPI {

    static AdvancementsAPI get() {
        RegisteredServiceProvider<AdvancementsAPI> provider = Bukkit.getServicesManager().getRegistration(AdvancementsAPI.class);
        return provider != null ? provider.getProvider() : null;
    }

    void registerCategory(CategoryModel category);
    CategoryModel getCategory(String id);
    Collection<CategoryModel> getAllCategories();

    void registerAdvancement(AdvancementModel model);
    Collection<AdvancementModel> getAllAdvancements();
    List<AdvancementModel> getByCategory(String categoryId);

    CompletableFuture<Boolean> grantAsync(Player player, String keyOrId);
    boolean grant(Player player, String keyOrId);
    boolean hasAdvancement(Player player, String keyOrId);

    int getCategoryCompletedCount(Player player, String categoryId);
    int getTotalCompletedCount(Player player);

    boolean isBedrockPlayer(Player player);
    void openBedrockMenu(Player player);
    NamespacedKey resolveKey(String query);
}