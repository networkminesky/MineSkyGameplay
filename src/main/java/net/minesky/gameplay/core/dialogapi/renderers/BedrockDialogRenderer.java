package net.minesky.gameplay.core.dialogapi.renderers;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.MineSkyGameplayPlugin;
import net.minesky.gameplay.api.dialogapi.DialogButton;
import net.minesky.gameplay.api.dialogapi.DialogInputField;
import net.minesky.gameplay.api.dialogapi.MineSkyDialog;
import net.minesky.gameplay.api.dialogapi.inputs.BoolInput;
import net.minesky.gameplay.api.dialogapi.inputs.DropdownInput;
import net.minesky.gameplay.api.dialogapi.inputs.SliderInput;
import net.minesky.gameplay.api.dialogapi.inputs.TextInput;
import net.minesky.gameplay.core.dialogapi.DialogComponentUtils;
import net.minesky.gameplay.core.dialogapi.responses.BedrockDialogResponse;
import net.minesky.gameplay.core.dialogapi.responses.EmptyDialogResponse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BedrockDialogRenderer {

    private BedrockDialogRenderer() {}

    public static void renderAndShow(MineSkyGameplayPlugin plugin, Player player, MineSkyDialog dialog) {
        FloodgatePlayer fPlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        if (fPlayer == null) return;

        if (dialog.hasInputs()) {
            renderCustomForm(plugin, player, fPlayer, dialog);
        } else if (dialog.getButtons().size() == 2) {
            renderModalForm(plugin, player, fPlayer, dialog);
        } else {
            renderSimpleForm(plugin, player, fPlayer, dialog);
        }
    }

    private static void renderModalForm(MineSkyGameplayPlugin plugin, Player player, FloodgatePlayer fPlayer, MineSkyDialog dialog) {
        DialogButton btn1 = dialog.getButtons().get(0);
        DialogButton btn2 = dialog.getButtons().get(1);

        StringBuilder content = new StringBuilder();
        for (Component c : dialog.getBody()) {
            if (!content.isEmpty()) content.append("\n");
            content.append(DialogComponentUtils.toBedrock(c));
        }

        ModalForm form = ModalForm.builder()
                .title(DialogComponentUtils.toBedrock(dialog.getTitle()))
                .content(content.toString())
                .button1(DialogComponentUtils.toBedrock(btn1.getLabel()))
                .button2(DialogComponentUtils.toBedrock(btn2.getLabel()))
                .validResultHandler(response -> player.getScheduler().run(plugin, task -> {
                    EmptyDialogResponse res = new EmptyDialogResponse(player);
                    if (response.clickedButtonId() == 0) {
                        btn1.click(player, res);
                    } else {
                        btn2.click(player, res);
                    }
                }, null))
                .closedResultHandler(res -> {
                    if (dialog.getCloseAction() != null) {
                        player.getScheduler().run(plugin, task -> dialog.getCloseAction().accept(player), null);
                    }
                })
                .build();

        fPlayer.sendForm(form);
    }

    private static void renderSimpleForm(MineSkyGameplayPlugin plugin, Player player, FloodgatePlayer fPlayer, MineSkyDialog dialog) {
        StringBuilder content = new StringBuilder();
        for (Component c : dialog.getBody()) {
            if (!content.isEmpty()) content.append("\n");
            content.append(DialogComponentUtils.toBedrock(c));
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(DialogComponentUtils.toBedrock(dialog.getTitle()))
                .content(content.toString());

        for (DialogButton btn : dialog.getButtons()) {
            String label = DialogComponentUtils.toBedrock(btn.getLabel());
            if (btn.getIconUrl() != null) {
                builder.button(label, FormImage.Type.URL, btn.getIconUrl());
            } else if (btn.getIconPath() != null) {
                builder.button(label, FormImage.Type.PATH, btn.getIconPath());
            } else {
                builder.button(label);
            }
        }

        builder.validResultHandler(response -> {
            int id = response.clickedButtonId();
            if (id >= 0 && id < dialog.getButtons().size()) {
                DialogButton clicked = dialog.getButtons().get(id);
                player.getScheduler().run(plugin, task -> clicked.click(player, new EmptyDialogResponse(player)), null);
            }
        });

        builder.closedResultHandler(res -> {
            if (dialog.getCloseAction() != null) {
                player.getScheduler().run(plugin, task -> dialog.getCloseAction().accept(player), null);
            }
        });

        fPlayer.sendForm(builder.build());
    }

    private static void renderCustomForm(MineSkyGameplayPlugin plugin, Player player, FloodgatePlayer fPlayer, MineSkyDialog dialog) {
        CustomForm.Builder builder = CustomForm.builder()
                .title(DialogComponentUtils.toBedrock(dialog.getTitle()));

        int elementIndex = 0;
        for (Component c : dialog.getBody()) {
            builder.label(DialogComponentUtils.toBedrock(c));
            elementIndex++;
        }

        for (ItemStack item : dialog.getItems()) {
            String name = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                    ? DialogComponentUtils.toBedrock(item.getItemMeta().displayName())
                    : item.getType().name();
            builder.label("§7[Item: §f" + name + " §7x" + item.getAmount() + "§7]");
            elementIndex++;
        }

        Map<Integer, DialogInputField<?>> indexMap = new HashMap<>();

        for (DialogInputField<?> input : dialog.getInputs()) {
            if (input instanceof TextInput text) {
                builder.input(DialogComponentUtils.toBedrock(text.getLabel()), text.getPlaceholder(), text.getDefaultValue());
                indexMap.put(elementIndex++, text);
            } else if (input instanceof BoolInput bool) {
                builder.toggle(DialogComponentUtils.toBedrock(bool.getLabel()), bool.getDefaultValue());
                indexMap.put(elementIndex++, bool);
            } else if (input instanceof SliderInput slider) {
                builder.slider(DialogComponentUtils.toBedrock(slider.getLabel()), slider.getMin(), slider.getMax(), slider.getStep(), slider.getDefaultValue());
                indexMap.put(elementIndex++, slider);
            } else if (input instanceof DropdownInput dropdown) {
                List<String> options = dropdown.getOptions().stream()
                        .map(opt -> DialogComponentUtils.toBedrock(opt.label()))
                        .toList();
                builder.dropdown(DialogComponentUtils.toBedrock(dropdown.getLabel()), options, dropdown.getDefaultIndex());
                indexMap.put(elementIndex++, dropdown);
            }
        }

        // Se houver múltiplos botões num CustomForm, Bedrock só tem 1 botão nativo de submit.
        // Adicionamos um dropdown de escolha de ação caso existam 2 ou mais botões:
        Integer actionDropdownIndex = null;
        if (dialog.getButtons().size() > 1) {
            List<String> buttonOptions = dialog.getButtons().stream()
                    .map(b -> DialogComponentUtils.toBedrock(b.getLabel()))
                    .toList();
            builder.dropdown("§eAção / Escolha", buttonOptions, 0);
            actionDropdownIndex = elementIndex++;
        }

        final Integer finalActionIdx = actionDropdownIndex;

        builder.validResultHandler(response -> {
            Map<String, Object> values = new HashMap<>();

            for (Map.Entry<Integer, DialogInputField<?>> entry : indexMap.entrySet()) {
                int idx = entry.getKey();
                DialogInputField<?> field = entry.getValue();

                if (field instanceof TextInput) {
                    values.put(field.getKey(), response.asInput(idx));
                } else if (field instanceof BoolInput) {
                    values.put(field.getKey(), response.asToggle(idx));
                } else if (field instanceof SliderInput) {
                    values.put(field.getKey(), response.asSlider(idx));
                } else if (field instanceof DropdownInput dropdown) {
                    int selected = response.asDropdown(idx);
                    if (selected >= 0 && selected < dropdown.getOptions().size()) {
                        values.put(field.getKey(), dropdown.getOptions().get(selected).id());
                    }
                }
            }

            BedrockDialogResponse dialogResponse = new BedrockDialogResponse(player, values);

            player.getScheduler().run(plugin, task -> {
                if (dialog.getSubmitAction() != null) {
                    dialog.getSubmitAction().accept(player, dialogResponse);
                }

                if (finalActionIdx != null) {
                    int chosenBtn = response.asDropdown(finalActionIdx);
                    if (chosenBtn >= 0 && chosenBtn < dialog.getButtons().size()) {
                        dialog.getButtons().get(chosenBtn).click(player, dialogResponse);
                    }
                } else if (!dialog.getButtons().isEmpty()) {
                    dialog.getButtons().get(0).click(player, dialogResponse);
                }
            }, null);
        });

        builder.closedResultHandler(res -> {
            if (dialog.getCloseAction() != null) {
                player.getScheduler().run(plugin, task -> dialog.getCloseAction().accept(player), null);
            }
        });

        fPlayer.sendForm(builder.build());
    }
}