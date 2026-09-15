package net.minesky.mineskygameplay.locatorapi;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * API para controle e manipulação da Locator Bar do Minecraft (Folia 26.1.2+).
 */
public interface LocatorAPI {

    /**
     * Obtém a instância ativa da LocatorAPI através do ServicesManager.
     * Retorna null caso o plugin principal (mineskygameplay) não esteja carregado.
     */
    static LocatorAPI get() {
        RegisteredServiceProvider<LocatorAPI> provider = Bukkit.getServicesManager().getRegistration(LocatorAPI.class);
        if (provider != null) {
            return provider.getProvider();
        }
        try {
            return LocatorManager.getInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ==========================================
    //            Controle de Mundos
    // ==========================================

    boolean isWorldEnabled(World world);
    boolean isWorldEnabled(String worldName);

    boolean isWorldDisabled(World world);
    boolean isWorldDisabled(String worldName);

    LocatorWorldMode getWorldMode(String worldName);
    void setWorldMode(String worldName, LocatorWorldMode mode);

    List<String> getAlwaysEnabledWorlds();
    List<String> getAlwaysDisabledWorlds();

    void reloadConfig();

    // ==========================================
    //       Verificações de Visibilidade
    // ==========================================

    /**
     * Verifica se o visualizador (viewer) tem permissão de ver o alvo (target) na locator bar.
     */
    boolean canSee(Player viewer, Player target);
    boolean canSee(UUID viewerId, UUID targetId, World world);

    // ==========================================
    //     Manipulação de Jogador para Jogador
    // ==========================================

    /**
     * Força o visualizador a ver o alvo na Locator Bar.
     * Funciona inclusive em mundos desativados (always-disabled-worlds).
     */
    void showPlayer(Player viewer, Player target);

    /**
     * Oculta o alvo da Locator Bar do visualizador.
     * Funciona inclusive em mundos ativados (always-enabled-worlds).
     */
    void hidePlayer(Player viewer, Player target);

    /**
     * Define a visibilidade do alvo para o visualizador.
     */
    void setPlayerVisibleTo(Player viewer, Player target, boolean visible);

    /**
     * Reseta quaisquer substituições individuais entre o visualizador e o alvo,
     * voltando às regras padrão do mundo.
     */
    void resetPlayerVisibility(Player viewer, Player target);

    /**
     * Limpa todas as regras personalizadas de visualização do jogador.
     */
    void clearPlayerOverrides(Player viewer);

    // ==========================================
    //      Modo Stealth / Revelação Global
    // ==========================================

    /**
     * Oculta o jogador de TODAS as Locator Bars do servidor (útil para Vanish/Staff/Assassins).
     */
    void hidePlayerGlobally(Player player);

    /**
     * Remove o modo stealth global do jogador.
     */
    void showPlayerGlobally(Player player);

    boolean isPlayerGloballyHidden(Player player);

    /**
     * Revela o jogador para TODOS no mesmo mundo (mesmo em mundos desativados).
     * Ideal para mecânicas de "Bounty", "Boss Player" ou "Portador da Bandeira".
     */
    void revealPlayerGlobally(Player player);

    /**
     * Remove a revelação forçada global.
     */
    void unrevealPlayerGlobally(Player player);

    boolean isPlayerGloballyRevealed(Player player);

    // ==========================================
    //        Grupos / Equipes / Partys
    // ==========================================

    /**
     * Cria um grupo de rastreamento (ex: Clan, Party, Squad).
     * Jogadores no mesmo grupo conseguem se enxergar na Locator Bar mesmo em mundos desativados.
     */
    void createGroup(String groupId);

    void deleteGroup(String groupId);

    void addPlayerToGroup(String groupId, Player player);

    void removePlayerFromGroup(Player player);

    Set<UUID> getGroupMembers(String groupId);

    Optional<String> getPlayerGroup(Player player);

    void setGroupMutualVisibility(String groupId, boolean visible);
}