package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.ftb_subchunks;

@Mixin(targets = "dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel$ChunkButton", remap = false)
public abstract class MixinChunkButton {

    @Shadow public abstract XZ getChunkPos();
    @Shadow private MapChunk chunk;

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void onDrawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        XZ xz = getChunkPos();
        LevelChunk levelChunk = mc.level.getChunkSource().getChunkNow(xz.x(), xz.z());
        if (levelChunk != null) {
            SubGroupData data = levelChunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);
            if (data != null && !data.getZones().isEmpty()) {
                Color4I gold = Color4I.rgb(0xFFD700);
                gold.draw(graphics, x, y, w, 2);
                gold.draw(graphics, x, y + h - 2, w, 2);
                gold.draw(graphics, x, y, 2, h);
                gold.draw(graphics, x + w - 2, y, 2, h);
                Color4I.rgb(0xFFD700).withAlpha(50).draw(graphics, x, y, w, h);
            }
        }
    }

    @Inject(method = "addMouseOverText", at = @At("TAIL"))
    private void addSubGroupInstruction(TooltipList list, CallbackInfo ci) {
        if (chunk != null && chunk.getTeam().isPresent() && chunk.isTeamMember(Minecraft.getInstance().player)) {
            Minecraft mc = Minecraft.getInstance();
            XZ xz = getChunkPos();
            LevelChunk levelChunk = mc.level != null ? mc.level.getChunkSource().getChunkNow(xz.x(), xz.z()) : null;

            if (levelChunk != null) {
                SubGroupData data = levelChunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);
                if (data != null && !data.getZones().isEmpty()) {
                    list.add(Component.literal("Sub-Group: " + data.getZones().get(0).getName()).withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }
}