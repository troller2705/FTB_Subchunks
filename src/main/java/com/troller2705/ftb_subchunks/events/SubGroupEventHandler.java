package com.troller2705.ftb_subchunks.events;

import com.troller2705.ftb_subchunks.ftb_subchunks;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.data.SubZone;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Optional;

@EventBusSubscriber(modid = ftb_subchunks.MODID)
public class SubGroupEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        if (shouldDeny(player, event.getPos(), "edit")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (shouldDeny(player, event.getPos(), "edit")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (shouldDeny(player, event.getPos(), "interact")) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldDeny(ServerPlayer player, BlockPos pos, String actionType) {
        LevelChunk chunk = player.level().getChunkAt(pos);
        SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);

        // If there are no Sub-Groups here, let standard FTB Chunks handle it
        if (data.getZones().isEmpty()) return false;

        // Get the FTB Team that owns this chunk
        // Convert the BlockPos into Chunk coordinates for the FTB constructor
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        // Use the ResourceKey and the raw Chunk X/Z
        dev.ftb.mods.ftblibrary.math.ChunkDimPos cdp = new dev.ftb.mods.ftblibrary.math.ChunkDimPos(player.level().dimension(), chunkX, chunkZ);

        dev.ftb.mods.ftbchunks.api.ClaimedChunk claimed = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api().getManager().getChunk(cdp);
        if (claimed == null) return false;

        Optional<Team> ownerTeam = FTBTeamsAPI.api().getManager().getTeamByID(claimed.getTeamData().getTeam().getTeamId());
        if (ownerTeam.isEmpty()) return false;

        // Get the player's rank relative to the owner of the land
        TeamRank playerRank = ownerTeam.get().getRankForPlayer(player.getUUID());

        for (SubZone zone : data.getZones()) {
            TeamRank requiredRank = actionType.equals("edit") ? zone.getBlockEditRank() : zone.getBlockInteractRank();

            // Logic: If player's rank (e.g. ALLY = 50) is LESS than required (e.g. MEMBER = 100), DENY.
            if (playerRank.getPower() < requiredRank.getPower()) {
                return true;
            }
        }

        return false;
    }
}