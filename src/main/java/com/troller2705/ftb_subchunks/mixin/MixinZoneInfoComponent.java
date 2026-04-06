package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.minimap.components.ZoneInfoComponent;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.data.SubZone;
import com.troller2705.ftb_subchunks.ftb_subchunks;

@Mixin(value = ZoneInfoComponent.class, remap = false)
public abstract class MixinZoneInfoComponent {

    // Redirect the moment the Minimap asks for the team's colored name
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/api/Team;getColoredName()Lnet/minecraft/network/chat/Component;"))
    private Component redirectMinimapTeamName(Team instance) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.level() != null) {
            BlockPos pos = player.blockPosition();

            // Use getChunkSource().getChunkNow to avoid any "Level vs World" accessor issues
            LevelChunk chunk = player.level().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);

            if (chunk != null) {
                SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);
                for (SubZone zone : data.getZones()) {
                    if (zone.contains(pos)) {
                        return Component.literal(zone.getName()).withStyle(ChatFormatting.GOLD);
                    }
                }
            }
        }
        return instance.getColoredName();
    }
}