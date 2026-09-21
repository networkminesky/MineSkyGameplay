package net.minesky.gameplay.features.homes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SafeLocationUtil {

    private SafeLocationUtil() {}

    public static Location findSafeLocation(World world, int x, int z) {
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5};
        for (int dx : offsets) {
            for (int dz : offsets) {
                int checkX = x + dx;
                int checkZ = z + dz;
                int topY = world.getHighestBlockYAt(checkX, checkZ);
                for (int y = Math.min(topY, world.getMaxHeight() - 2); y >= world.getMinHeight() + 1; y--) {
                    Block ground = world.getBlockAt(checkX, y, checkZ);
                    Block feet = world.getBlockAt(checkX, y + 1, checkZ);
                    Block head = world.getBlockAt(checkX, y + 2, checkZ);

                    if (isSafeGround(ground) && isPassable(feet) && isPassable(head)) {
                        return new Location(world, checkX + 0.5, y + 1.0, checkZ + 0.5);
                    }
                }
            }
        }
        return new Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1.0, z + 0.5);
    }

    private static boolean isSafeGround(Block block) {
        Material type = block.getType();
        return type.isSolid()
                && type != Material.LAVA
                && type != Material.MAGMA_BLOCK
                && type != Material.CACTUS
                && type != Material.CAMPFIRE
                && type != Material.SOUL_CAMPFIRE;
    }

    private static boolean isPassable(Block block) {
        Material type = block.getType();
        return !type.isSolid()
                && type != Material.LAVA
                && type != Material.FIRE
                && type != Material.SOUL_FIRE;
    }
}