package net.minesky.gameplay.api;

import net.minesky.gameplay.api.advancements.AdvancementsAPI;
import net.minesky.gameplay.api.locator.LocatorAPI;

public final class MineSkyAPI {

    private MineSkyAPI() {}

    public static LocatorAPI getLocator() {
        return LocatorAPI.get();
    }

    public static AdvancementsAPI getAdvancements() {
        return AdvancementsAPI.get();
    }
}