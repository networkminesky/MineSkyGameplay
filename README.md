# MineSkyGameplay
Plugin pra deixar a experiência do usuário inicial mais amigável e melhorar a gameplay de modo geral.

---

# 🧭 LocatorAPI — Documentação Técnica & Guia de Uso

API de alta performance desenvolvida para **Folia 26.1.2** (utilizando Mojang Mappings / Paperweight) para manipulação, filtragem e controle dinâmico da **Locator Bar** nativa do Minecraft.

---

## 📑 Sumário

1. [Como a Locator Bar e a API Funcionam](#-como-a-locator-bar-e-a-api-funcionam)
2. [Compatibilidade com Folia & Plugman](#-compatibilidade-com-folia--plugman)
3. [Configuração (`config.yml`)](#-configuração-configyml)
4. [Como Integrar em Outros Plugins](#-como-integrar-em-outros-plugins)
   - [Dependência no Gradle / Maven](#dependência-no-gradle--maven)
   - [Declaração no `plugin.yml`](#declaração-no-pluginyml)
5. [Referência Completa de Métodos](#-referência-completa-de-métodos)
   - [1. Obtenção da Instância](#1-obtenção-da-instância)
   - [2. Controle e Consulta de Mundos](#2-controle-e-consulta-de-mundos)
   - [3. Checagem de Visibilidade](#3-checagem-de-visibilidade)
   - [4. Manipulação Jogador-para-Jogador](#4-manipulação-jogador-para-jogador)
   - [5. Modos Globais (Stealth e Revelação)](#5-modos-globais-stealth-e-revelação)
   - [6. Sistema de Grupos / Partys / Clãs](#6-sistema-de-grupos--partys--clãs)
6. [Exemplos Práticos de Cenários de Uso](#-exemplos-práticos-de-cenários-de-uso)

---

## 🧠 Como a Locator Bar e a API Funcionam

No cliente do Minecraft (a partir das versões 26.x):
* A **Locator Bar** substitui visualmente a barra de experiência (XP) quando há indicadores direcionais (waypoints) de jogadores ativos.
* Se um jogador recebe **0 waypoints**, o cliente desliga a barra direcional e renderiza a **barra de XP clássica normal**.
* Ao receber **1 ou mais waypoints**, o cliente ativa a **Locator Bar** e renderiza os marcadores direcionais na bússola superior/inferior.

### O Mecanismo da API:
A **LocatorAPI** atua na camada de rede (Netty) interceptando pacotes `ClientboundTrackedWaypointPacket`:
1. **Em Mundos Desativados (`always-disabled-worlds`)**: A API descarta os pacotes de waypoints por padrão. O jogador mantém sua barra de XP comum. Quando você utiliza métodos da API (como `showPlayer` ou adiciona jogadores a um mesmo grupo), apenas os pacotes desses jogadores específicos são autorizados, fazendo com que **somente eles apareçam na Locator Bar**.
2. **Em Mundos Ativados (`always-enabled-worlds`)**: O fluxo é liberado por padrão (comportamento vanilla), permitindo que métodos como `hidePlayer` ou `hidePlayerGlobally` ocultem jogadores seletivamente.
3. **Conversão Proativa de Pacotes**: Se o servidor tentar enviar uma atualização (`UPDATE`) para um cliente que ainda não havia registrado o alvo via `TRACK`, a API reescreve o pacote em tempo real para `TRACK`, evitando desincronizações visuais e erros de protocolo.

---

## ⚙️ Configuração (`config.yml`)

O arquivo de configuração reside exclusivamente no plugin principal (`mineskygameplay`):

```yaml
# =========================================================
#             Configuração da LocatorAPI (Folia)
# =========================================================

# Mundos onde a Locator Bar ficará SEMPRE LIGADA por padrão.
# Todos os jogadores se enxergam, a não ser que sejam ocultados via API.
always-enabled-worlds:
  - "minesky_the_end"

# Mundos onde a Locator Bar ficará SEMPRE DESLIGADA por padrão.
# A barra exibe a XP normal. Outros jogadores só aparecem na barra
# se forem especificamente liberados via API ou grupos.
always-disabled-worlds:
  - "minesky_nether"
  - "minesky_overworld"

# Modo padrão para mundos não listados acima (ENABLED ou DISABLED)
default-world-mode: "ENABLED"

# Logs adicionais no console para depuração de pacotes
debug: false
```

---

## 📦 Como Integrar em Outros Plugins

### Dependência no Gradle / Maven

Use o JitPack:
https://jitpack.io/#networkminesky/mineskygameplay

### Declaração no `plugin.yml`

No `plugin.yml` (ou `paper-plugin.yml`) do outro plugin:

```yaml
name: "MineSkyClans"
version: "1.0.0"
main: "net.minesky.clans.MineSkyClans"

# Garante que o mineskygameplay inicie antes e registre a API
depend:
  - mineskygameplay
```

---

## 📚 Referência Completa de Métodos

Importe a interface:
```java
import net.minesky.mineskygameplay.locatorapi.LocatorAPI;
import net.minesky.mineskygameplay.locatorapi.LocatorWorldMode;
```

---

### 1. Obtenção da Instância

#### `LocatorAPI.get()`
Retorna a instância singleton ativa registrada no Bukkit `ServicesManager`.
```java
LocatorAPI api = LocatorAPI.get();
if (api == null) {
    // mineskygameplay não está carregado ou falhou na inicialização
}
```

---

### 2. Controle e Consulta de Mundos

#### `boolean isWorldEnabled(World world)` / `boolean isWorldEnabled(String worldName)`
Retorna se a Locator Bar do mundo está configurada para funcionar ativada por padrão.
```java
boolean ativado = api.isWorldEnabled(player.getWorld());
```

#### `boolean isWorldDisabled(World world)` / `boolean isWorldDisabled(String worldName)`
Retorna se o mundo desliga a Locator Bar por padrão (exibindo a barra de XP vanilla).
```java
if (api.isWorldDisabled(player.getWorld())) {
    // Jogadores comuns não se veem aqui por padrão
}
```

#### `LocatorWorldMode getWorldMode(String worldName)`
Retorna o enum de modo do mundo: `ALWAYS_ENABLED`, `ALWAYS_DISABLED` ou `DEFAULT`.
```java
LocatorWorldMode mode = api.getWorldMode("minesky_overworld");
```

#### `void setWorldMode(String worldName, LocatorWorldMode mode)`
Altera dinamicamente em tempo de execução a regra de um mundo específico.
```java
api.setWorldMode("arena_pvp", LocatorWorldMode.ALWAYS_DISABLED);
```

#### `List<String> getAlwaysEnabledWorlds()`
Retorna a lista de nomes dos mundos onde a Locator Bar fica sempre ligada.

#### `List<String> getAlwaysDisabledWorlds()`
Retorna a lista de nomes dos mundos onde a Locator Bar fica sempre desligada.

#### `void reloadConfig()`
Recarrega as listas de mundos e configurações a partir do `config.yml` do disco.
```java
api.reloadConfig();
```

---

### 3. Checagem de Visibilidade

#### `boolean canSee(Player viewer, Player target)`
Verifica se `viewer` consegue enxergar o marcador de `target` na sua Locator Bar no momento exato, considerando regras do mundo, grupos, stealth e revelações.
```java
if (api.canSee(p1, p2)) {
    p1.sendMessage("Você está rastreando " + p2.getName());
}
```

#### `boolean canSee(UUID viewerId, UUID targetId, World world)`
Versão de baixo nível para checagens assíncronas por UUID em uma instância de mundo.

---

### 4. Manipulação Jogador-para-Jogador

#### `void showPlayer(Player viewer, Player target)`
Força o `viewer` a enxergar o `target` na Locator Bar, **mesmo que o mundo seja desativado por padrão**.
* O alvo é sincronizado imediatamente para o cliente.
```java
// O Caçador agora vê a presa na barra dele, mesmo no Overworld
api.showPlayer(hunterPlayer, targetPlayer);
```

#### `void hidePlayer(Player viewer, Player target)`
Oculta o `target` da Locator Bar do `viewer`, **mesmo que o mundo seja ativado por padrão**.
* Envia um pacote de remoção (`UNTRACK`) imediatamente ao cliente.
```java
api.hidePlayer(viewer, target);
```

#### `void setPlayerVisibleTo(Player viewer, Player target, boolean visible)`
Define programaticamente o estado de visibilidade de um alvo para um observador.
```java
api.setPlayerVisibleTo(viewer, target, false); // Equivalente a hidePlayer
api.setPlayerVisibleTo(viewer, target, true);  // Equivalente a showPlayer
```

#### `void resetPlayerVisibility(Player viewer, Player target)`
Remove quaisquer preferências individuais definidas entre o par de jogadores, fazendo com que passem a obedecer apenas às regras padrão do mundo ou grupo.
```java
api.resetPlayerVisibility(viewer, target);
```

#### `void clearPlayerOverrides(Player viewer)`
Limpa todas as substituições individuais que foram configuradas para o visualizador informado.

---

### 5. Modos Globais (Stealth e Revelação)

#### `void hidePlayerGlobally(Player player)`
**Modo Stealth / Vanish Total**: Remove o jogador imediatamente da Locator Bar de **todos** os outros jogadores do servidor, em qualquer mundo.
```java
api.hidePlayerGlobally(staffPlayer);
```

#### `void showPlayerGlobally(Player player)`
Remove o modo Stealth global do jogador, restaurando sua visibilidade normal.
```java
api.showPlayerGlobally(staffPlayer);
```

#### `boolean isPlayerGloballyHidden(Player player)`
Retorna se o jogador está com o modo Stealth global ativado.

#### `void revealPlayerGlobally(Player player)`
**Modo Alvo / Bounty / Beacon**: Faz com que o jogador fique visível na Locator Bar de **todos** os jogadores presentes no mesmo mundo, independentemente do mundo estar desativado por padrão.
```java
api.revealPlayerGlobally(procuradoPlayer);
```

#### `void unrevealPlayerGlobally(Player player)`
Remove o estado de revelação forçada global do jogador.
```java
api.unrevealPlayerGlobally(procuradoPlayer);
```

#### `boolean isPlayerGloballyRevealed(Player player)`
Retorna se o jogador está marcado com revelação global.

---

### 6. Sistema de Grupos / Partys / Clãs

A API possui um motor interno de agrupamento. Jogadores pertencentes ao mesmo `groupId` se enxergam automaticamente na Locator Bar, mesmo dentro de mundos desativados (`always-disabled-worlds`), sem que outros jogadores de fora os vejam.

#### `void createGroup(String groupId)`
Cria um novo grupo de rastreamento com visibilidade mútua habilitada.
```java
api.createGroup("clan_shadows");
```

#### `void deleteGroup(String groupId)`
Remove o grupo e desvincula todos os membros, atualizando suas barras de localização.
```java
api.deleteGroup("clan_shadows");
```

#### `void addPlayerToGroup(String groupId, Player player)`
Adiciona um jogador ao grupo. Se o grupo não existir, ele é criado automaticamente. O jogador é automaticamente desvinculado de qualquer outro grupo anterior.
```java
api.addPlayerToGroup("party_105", player1);
api.addPlayerToGroup("party_105", player2);
```

#### `void removePlayerFromGroup(Player player)`
Remove o jogador do grupo no qual ele se encontra.
```java
api.removePlayerFromGroup(player);
```

#### `Set<UUID> getGroupMembers(String groupId)`
Retorna um conjunto imutável com os UUIDs de todos os integrantes do grupo.

#### `Optional<String> getPlayerGroup(Player player)`
Retorna o ID do grupo atual do jogador, se houver.
```java
api.getPlayerGroup(player).ifPresent(group -> {
    player.sendMessage("Seu grupo de rastreamento: " + group);
});
```

#### `void setGroupMutualVisibility(String groupId, boolean visible)`
Define se membros do mesmo grupo podem se enxergar mutuamente. Se `false`, o grupo continua existindo, mas os membros não aparecem na Locator Bar uns dos outros.
```java
api.setGroupMutualVisibility("party_105", false);
```

---

## 💡 Exemplos Práticos de Cenários de Uso

### Cenário 1: Integração com Plugin de Clãs (Overworld Desativado)
Você quer que no mundo `minesky_overworld` (onde a Locator Bar é desativada por padrão) apenas aliados do mesmo clã se vejam:

```java
public class ClanEvents implements Listener {

    private final LocatorAPI api = LocatorAPI.get();

    public void aoEntrarNoClan(Player jogador, String clanTag) {
        // Vincula o jogador ao grupo do clã
        api.addPlayerToGroup("clan_" + clanTag.toLowerCase(), jogador);
    }

    public void aoSairDoClan(Player jogador) {
        api.removePlayerFromGroup(jogador);
    }
}
```

---

### Cenário 2: Item Mágico / Rastreador de Recompensas
Um item de "Bússola de Caçador" que faz o caçador rastrear temporariamente sua presa:

```java
public void ativarRastreamento(Player cacador, Player alvo, Plugin plugin) {
    LocatorAPI api = LocatorAPI.get();

    // Libera a presa exclusivamente na bússola/locator bar do caçador
    api.showPlayer(cacador, alvo);
    cacador.sendMessage("§aVocê está rastreando " + alvo.getName() + " por 30 segundos!");

    // No Folia: agenda o cancelamento no scheduler da entidade
    cacador.getScheduler().runDelayed(plugin, task -> {
        api.resetPlayerVisibility(cacador, alvo);
        cacador.sendMessage("§cO rastro de " + alvo.getName() + " desapareceu!");
    }, null, 30 * 20L);
}
```

---

### Cenário 3: Modo Staff Vanish
Ocultando membros da equipe de qualquer radar na Locator Bar:

```java
public void toggleVanish(Player staff) {
    LocatorAPI api = LocatorAPI.get();

    if (api.isPlayerGloballyHidden(staff)) {
        api.showPlayerGlobally(staff);
        staff.sendMessage("§aLocator Bar: Você voltou a ser visível!");
    } else {
        api.hidePlayerGlobally(staff);
        staff.sendMessage("§cLocator Bar: Você está completamente invisível!");
    }
}
```