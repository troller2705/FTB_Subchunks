package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.troller2705.ftb_subchunks.client.SubGroupClientUI;

@Mixin(value = ChunkScreen.class, remap = false)
public abstract class MixinChunkScreen extends dev.ftb.mods.ftblibrary.ui.BaseScreen {

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void addPaintbrushButton(CallbackInfo ci) {
        SimpleButton paintbrushBtn = new SimpleButton(this, Component.empty(), Icons.SETTINGS, (btn, mb) -> {
            if (mb.isRight()) {
                SubGroupClientUI.activePaintbrush = null;
                btn.playClickSound();
            } else {
                // FIX: Open the Palette List instead of the direct editor!
                SubGroupClientUI.openPaletteMenu(this::openGui);
            }
        }) {
            // NEW: Make the tooltip dynamic so you know exactly what mode you are in
            @Override
            public void addMouseOverText(dev.ftb.mods.ftblibrary.util.TooltipList list) {
                list.add(Component.literal("Sub-Group Paintbrush").withStyle(ChatFormatting.GOLD));
                if (SubGroupClientUI.activePaintbrush != null) {
                    // FIX: Added .getName() since activePaintbrush is a SubZone object now
                    list.add(Component.literal("Equipped: " + SubGroupClientUI.activePaintbrush.getName()).withStyle(ChatFormatting.GREEN));
                    list.add(Component.literal("Right-Click to disable").withStyle(ChatFormatting.RED));
                } else {
                    list.add(Component.literal("Left-Click to equip").withStyle(ChatFormatting.GRAY));
                }
            }
        };

        paintbrushBtn.setPosAndSize(-getX() + 40, -getY() + 2, 16, 16);
        add(paintbrushBtn);
    }
}