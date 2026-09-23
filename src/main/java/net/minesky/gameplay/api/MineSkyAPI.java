package net.minesky.gameplay.api;

import net.minesky.gameplay.api.advancements.AdvancementsAPI;
import net.minesky.gameplay.api.dialogapi.DialogAPI;
import net.minesky.gameplay.api.locator.LocatorAPI;

public final class MineSkyAPI {

    private MineSkyAPI() {}

    public static LocatorAPI getLocator() {
        return LocatorAPI.get();
    }

    public static AdvancementsAPI getAdvancements() {
        return AdvancementsAPI.get();
    }

    public static DialogAPI getDialog() {
        return DialogAPI.get();
    }

    public static DialogAPI dialog() { return DialogAPI.get(); }
    public static AdvancementsAPI advancements() { return AdvancementsAPI.get(); }
    public static LocatorAPI locator() { return LocatorAPI.get(); }
}