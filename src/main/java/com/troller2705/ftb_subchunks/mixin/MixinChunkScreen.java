package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.troller2705.ftb_subchunks.client.SubGroupClientUI;

@Mixin(value = ChunkScreen.class, remap = false)
public abstract class MixinChunkScreen extends dev.ftb.mods.ftblibrary.ui.BaseScreen {
    private SimpleButton subGroupToggleBtn;

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void addSubGroupToggle(CallbackInfo ci) {
        subGroupToggleBtn = new SimpleButton(this,
                Component.literal("Sub-Group Mode: " + (SubGroupClientUI.isSubGroupMode ? "ON" : "OFF"))
                        .withStyle(SubGroupClientUI.isSubGroupMode ? ChatFormatting.GREEN : ChatFormatting.RED),
                Icons.SETTINGS,
                (b, m) -> {
                    SubGroupClientUI.isSubGroupMode = !SubGroupClientUI.isSubGroupMode; // Toggle it!
                    b.setTitle(Component.literal("Sub-Group Mode: " + (SubGroupClientUI.isSubGroupMode ? "ON" : "OFF"))
                            .withStyle(SubGroupClientUI.isSubGroupMode ? ChatFormatting.GREEN : ChatFormatting.RED));
                });
        add(subGroupToggleBtn);
    }

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void alignSubGroupToggle(CallbackInfo ci) {
        // Place it right next to the Large Map button
        subGroupToggleBtn.setPosAndSize(-getX() + 20, -getY() + 2, 115, 16);
    }
}