package net.minesky.mineskygameplay.advancements.menu;

import net.minesky.mineskygameplay.MineSkyGameplay;
import net.minesky.mineskygameplay.advancements.AdvancementsAPI;
import net.minesky.mineskygameplay.advancements.data.AdvancementModel;
import net.minesky.mineskygameplay.advancements.data.CategoryModel;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

public class BedrockMenuManager {

    private final MineSkyGameplay plugin;

    public BedrockMenuManager(MineSkyGameplay plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        player.getScheduler().run(plugin, task -> {
            AdvancementsAPI api = AdvancementsAPI.get();
            int total = api.getAllAdvancements().size();
            int completed = api.getTotalCompletedCount(player);
            int percent = total > 0 ? (completed * 100) / total : 0;

            String content = "§7Bem-vindo ao painel de conquistas do §6Minesky§7!\n\n"
                    + "§fProgresso Geral: §a" + completed + "§7/§e" + total + " §8(" + percent + "%)\n"
                    + "§7Selecione uma categoria abaixo:";

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title("§6§lMINESKY §8- §eCONQUISTAS")
                    .content(content);

            List<CategoryModel> categories = new ArrayList<>(api.getAllCategories());
            for (CategoryModel cat : categories) {
                int catTotal = api.getByCategory(cat.id()).size();
                int catDone = api.getCategoryCompletedCount(player, cat.id());
                builder.button(cat.color() + "§l" + cat.title() + "\n§8Progresso: " + catDone + "/" + catTotal);
            }

            builder.button("§c§lFechar");

            builder.validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                if (clicked < categories.size()) {
                    openCategoryMenu(player, categories.get(clicked));
                }
            });

            FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
        }, null);
    }

    public void openCategoryMenu(Player player, CategoryModel category) {
        player.getScheduler().run(plugin, task -> {
            AdvancementsAPI api = AdvancementsAPI.get();
            List<AdvancementModel> list = api.getByCategory(category.id());
            int catTotal = list.size();
            int catDone = api.getCategoryCompletedCount(player, category.id());
            int percent = catTotal > 0 ? (catDone * 100) / catTotal : 0;

            String content = "§7" + category.description() + "\n\n"
                    + "§fProgresso: " + category.color() + catDone + "§7/§f" + catTotal + " §8(" + percent + "%)\n"
                    + "§7Clique em uma conquista para detalhes:";

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title(category.color() + "§l" + category.title())
                    .content(content);

            for (AdvancementModel adv : list) {
                boolean done = api.hasAdvancement(player, adv.id());
                if (done) {
                    builder.button("§a✔ " + adv.title() + "\n§2[CONCLUÍDO]");
                } else {
                    builder.button("§c✖ §7" + adv.title() + "\n§8[" + adv.frame().getDisplayName() + "]");
                }
            }

            builder.button("§e« Voltar ao Menu");

            builder.validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                if (clicked < list.size()) {
                    openDetailMenu(player, list.get(clicked), category);
                } else if (clicked == list.size()) {
                    openMainMenu(player);
                }
            });

            FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
        }, null);
    }

    public void openDetailMenu(Player player, AdvancementModel adv, CategoryModel category) {
        player.getScheduler().run(plugin, task -> {
            AdvancementsAPI api = AdvancementsAPI.get();
            boolean done = api.hasAdvancement(player, adv.id());

            StringBuilder sb = new StringBuilder();
            sb.append("§7Categoria: ").append(category.color()).append(category.title()).append("\n");
            sb.append("§7Tipo: ").append(adv.frame().getColor()).append(adv.frame().getDisplayName()).append("\n");
            sb.append("§7Status: ").append(done ? "§a✔ Concluído!" : "§c✖ Bloqueado").append("\n\n");
            sb.append("§eDescrição:\n§f").append(adv.description()).append("\n\n");

            if (adv.xpReward() > 0) {
                sb.append("§6Recompensa: §e+").append(adv.xpReward()).append(" XP\n");
            }

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title(adv.frame().getColor() + "§l" + adv.title())
                    .content(sb.toString())
                    .button("§e« Voltar para " + category.title())
                    .button("§cFechar");

            builder.validResultHandler(response -> {
                if (response.clickedButtonId() == 0) {
                    openCategoryMenu(player, category);
                }
            });

            FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
        }, null);
    }
}