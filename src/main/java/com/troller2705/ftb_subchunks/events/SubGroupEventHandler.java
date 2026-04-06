package com.troller2705.ftb_subchunks.events;

import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.TeamRank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import com.troller2705.ftb_subchunks.ftb_subchunks;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.data.SubZone;

public class SubGroupEventHandler {

    private static TeamRank getPlayerRank(Level level, BlockPos pos, Player player) {
        ClaimedChunk claimedChunk = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level, pos));
        if (claimedChunk != null && claimedChunk.getTeamData() != null) {
            return claimedChunk.getTeamData().getTeam().getRankForPlayer(player.getUUID());
        }
        return TeamRank.NONE;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();
        LevelChunk chunk = level.getChunkAt(pos);
        SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);

        if (data != null && !data.getZones().isEmpty()) {
            TeamRank playerRank = getPlayerRank(level, pos, player);

            for (SubZone zone : data.getZones()) {
                if (zone.contains(pos)) {
                    // Check against minimum rank requirement
                    if (!zone.canEditBlocks(playerRank)) {
                        event.setCanceled(true);
                        player.displayClientMessage(Component.literal("§cYour rank (" + playerRank.getSerializedName() + ") cannot edit blocks in " + zone.getName() + "!"), true);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        LevelChunk chunk = level.getChunkAt(pos);
        SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);

        if (data != null && !data.getZones().isEmpty()) {
            TeamRank playerRank = getPlayerRank(level, pos, player);

            for (SubZone zone : data.getZones()) {
                if (zone.contains(pos)) {
                    if (!zone.canInteractBlocks(playerRank)) {
                        event.setCanceled(true);
                        player.displayClientMessage(Component.literal("§cYour rank (" + playerRank.getSerializedName() + ") cannot interact with blocks in " + zone.getName() + "!"), true);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        BlockPos pos = event.getTarget().blockPosition();
        Level level = event.getLevel();
        LevelChunk chunk = level.getChunkAt(pos);
        SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);

        if (data != null && !data.getZones().isEmpty()) {
            TeamRank playerRank = getPlayerRank(level, pos, player);

            for (SubZone zone : data.getZones()) {
                if (zone.contains(pos)) {
                    if (!zone.canInteractEntities(playerRank)) {
                        event.setCanceled(true);
                        player.displayClientMessage(Component.literal("§cYour rank (" + playerRank.getSerializedName() + ") cannot interact with entities in " + zone.getName() + "!"), true);
                        return;
                    }
                }
            }
        }
    }
}