package net.minesky.mineskygameplay.advancements.loader;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minesky.mineskygameplay.MineSkyGameplay;
import net.minesky.mineskygameplay.advancements.AdvancementsAPI;
import net.minesky.mineskygameplay.advancements.data.AdvancementFrame;
import net.minesky.mineskygameplay.advancements.data.AdvancementModel;
import net.minesky.mineskygameplay.advancements.data.CategoryModel;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class DynamicAdvancementLoader {

    private static final String TARGET_NAMESPACE = "minesky";
    private final MineSkyGameplay plugin;
    private final AdvancementsAPI api;

    public DynamicAdvancementLoader(MineSkyGameplay plugin, AdvancementsAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    /**
     * Lê diretamente do ServerAdvancementManager do NMS e popula a API.
     */
    public synchronized void loadFromDatapack() {
        plugin.getLogger().info("Iniciando leitura dinâmica das conquistas do namespace '" + TARGET_NAMESPACE + "' via NMS...");

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            plugin.getLogger().warning("MinecraftServer ainda não está disponível.");
            return;
        }

        ServerAdvancementManager manager = server.getAdvancements();
        Collection<AdvancementHolder> allAdvancements = manager.getAllAdvancements();

        // Limpa o cache anterior para suportar reloads
        api.clearRegistry();

        Map<String, AdvancementHolder> rootMap = new HashMap<>();
        List<AdvancementHolder> childAdvancements = new ArrayList<>();

        // Passo 1: Separar raízes de categorias e conquistas filhas
        for (AdvancementHolder holder : allAdvancements) {
            Identifier id = holder.id();
            if (!id.getNamespace().equalsIgnoreCase(TARGET_NAMESPACE)) {
                continue;
            }

            String path = id.getPath(); // ex: "sociedade/root" ou "sociedade/protegido"
            if (path.endsWith("/root")) {
                String categoryId = path.substring(0, path.indexOf("/root"));
                rootMap.put(categoryId, holder);
            } else {
                childAdvancements.add(holder);
            }
        }

        // Passo 2: Registrar Categorias Dinâmicas a partir das raízes
        for (Map.Entry<String, AdvancementHolder> entry : rootMap.entrySet()) {
            String categoryId = entry.getKey();
            AdvancementHolder rootHolder = entry.getValue();

            Optional<DisplayInfo> displayOpt = rootHolder.value().display();
            String title = categoryId.substring(0, 1).toUpperCase() + categoryId.substring(1);
            String desc = "Conquistas de " + title;

            if (displayOpt.isPresent()) {
                DisplayInfo display = displayOpt.get();
                title = LegacyComponentSerializer.legacySection().serialize(
                        PaperAdventure.asAdventure(display.getTitle())
                );
                desc = LegacyComponentSerializer.legacySection().serialize(
                        PaperAdventure.asAdventure(display.getDescription())
                );
            }

            String color = getCategoryColor(categoryId);
            api.registerCategory(new CategoryModel(categoryId, title, color, desc));
        }

        // Passo 3: Registrar Conquistas Filhas
        int registeredCount = 0;
        for (AdvancementHolder holder : childAdvancements) {
            Identifier id = holder.id();
            String path = id.getPath();

            int slashIndex = path.indexOf('/');
            String categoryId = slashIndex != -1 ? path.substring(0, slashIndex) : "geral";
            String advId = slashIndex != -1 ? path.substring(slashIndex + 1) : path;

            // Garante que a categoria exista mesmo se o root não foi configurado
            if (api.getCategory(categoryId) == null) {
                api.registerCategory(new CategoryModel(
                        categoryId,
                        categoryId.substring(0, 1).toUpperCase() + categoryId.substring(1),
                        getCategoryColor(categoryId),
                        "Conquistas diversas"
                ));
            }

            Optional<DisplayInfo> displayOpt = holder.value().display();
            String title = advId;
            String desc = "";
            AdvancementFrame frame = AdvancementFrame.TASK;
            ItemStack icon = null;

            if (displayOpt.isPresent()) {
                DisplayInfo display = displayOpt.get();
                title = LegacyComponentSerializer.legacySection().serialize(
                        PaperAdventure.asAdventure(display.getTitle())
                );
                desc = LegacyComponentSerializer.legacySection().serialize(
                        PaperAdventure.asAdventure(display.getDescription())
                );

                // Mapeamento do tipo de frame NMS -> Frame do Plugin
                AdvancementType type = display.getType();
                if (type == AdvancementType.CHALLENGE) {
                    frame = AdvancementFrame.CHALLENGE;
                } else if (type == AdvancementType.GOAL) {
                    frame = AdvancementFrame.GOAL;
                } else {
                    frame = AdvancementFrame.TASK;
                }

                // Ícone NMS convertido para Bukkit ItemStack
                icon = CraftItemStack.asBukkitCopy(display.getIcon());
            }

            // Recompensa de XP direto do NMS
            int xpReward = holder.value().rewards().experience();

            NamespacedKey key = new NamespacedKey(TARGET_NAMESPACE, path);
            AdvancementModel model = new AdvancementModel(advId, categoryId, key, title, desc, frame, xpReward, icon);

            api.registerAdvancement(model);
            registeredCount++;
        }

        plugin.getLogger().info(String.format(
                "Sucesso! %d categorias dinâmicas e %d conquistas carregadas diretamente do datapack.",
                api.getAllCategories().size(), registeredCount
        ));
    }

    private String getCategoryColor(String categoryId) {
        return switch (categoryId.toLowerCase()) {
            case "sociedade" -> "§6";
            case "mineracao" -> "§b";
            case "invencao" -> "§e";
            case "raids" -> "§c";
            default -> "§a";
        };
    }
}
