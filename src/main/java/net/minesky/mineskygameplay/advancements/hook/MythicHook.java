package net.minesky.mineskygameplay.advancements.hook;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import net.minesky.mineskygameplay.MineSkyGameplay;
import net.minesky.mineskygameplay.advancements.AdvancementsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;

public class MythicHook implements Listener {

    public static void register(MineSkyGameplay plugin) {
        Bukkit.getPluginManager().registerEvents(new MythicHook(), plugin);
    }

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event) {
        String mechanicName = event.getMechanicName().toLowerCase();

        switch (mechanicName) {
            case "advancementapi":
            case "advancementsapi":
            case "grantadvancement":
            case "giveadvancement":
            case "mineskygameplay":
                event.register(new AdvancementMechanic(
                        event.getContainer().getManager(),
                        event.getContainer().getFile(),
                        event.getContainer().getConfigLine(),
                        event.getConfig()
                ));
                break;
        }
    }

    public static class AdvancementMechanic extends SkillMechanic implements ITargetedEntitySkill {
        private final PlaceholderString key;

        public AdvancementMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
            super(manager, file, line, mlc);
            this.key = mlc.getPlaceholderString(new String[]{"id", "advancement", "achievment", "a", "i", "key", "name"}, "minecraft:root");
        }

        @Override
        public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
            if (target.isPlayer()) {
                Player p = (Player) target.getBukkitEntity();
                String resolvedKey = this.key.get(data, target);
                AdvancementsAPI.get().grantAsync(p, resolvedKey);
            }
            return SkillResult.SUCCESS;
        }
    }
}