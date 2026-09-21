package net.minesky.gameplay.features.homes;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import net.william278.huskclaims.api.BukkitHuskClaimsAPI;
import net.william278.huskclaims.user.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class HomesHook {

    public void getClaims(Player player) {
        OnlineUser user = BukkitHuskClaimsAPI.getInstance().getOnlineUser(player);
        BukkitHuskClaimsAPI.getInstance().getGlobalUserClaims(user).thenAcceptAsync((list) -> {

        });
    }

    public List<String> getHomes(Player player) {
        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if(essentials == null)
            return List.of();

        User user = essentials.getUser(player);
        if (user == null) {
            return List.of();
        }

        return user.getHomes();
    }

}
