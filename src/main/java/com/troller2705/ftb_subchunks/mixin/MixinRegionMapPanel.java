package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.data.SubZone;
import com.troller2705.ftb_subchunks.ftb_subchunks;

@Mixin(value = RegionMapPanel.class, remap = false)
public abstract class MixinRegionMapPanel {

    // Shadow the variables FTB Chunks uses to track the mouse position
    @Shadow int blockX;
    @Shadow int blockY;
    @Shadow int blockZ;

    @Redirect(method = "addMouseOverText", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/api/Team;getName()Lnet/minecraft/network/chat/Component;"))
    private Component redirectLargeMapTeamName(Team instance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            // If the map hasn't loaded height data (-32767), default to world bottom
            int fixedY = blockY == -32767 ? mc.level.getMinBuildHeight() : blockY;
            BlockPos pos = new BlockPos(blockX, fixedY, blockZ);
            LevelChunk chunk = mc.level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);

            if (chunk != null) {
                SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);
                if (data != null && !data.getZones().isEmpty()) {
                    for (SubZone zone : data.getZones()) {
                        if (zone.contains(pos)) {
                            return Component.literal(zone.getName()).withStyle(ChatFormatting.GOLD);
                        }
                    }
                }
            }
        }
        return instance.getName();
    }
}