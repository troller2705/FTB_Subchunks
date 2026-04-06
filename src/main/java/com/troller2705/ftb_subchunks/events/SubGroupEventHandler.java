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
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

import java.util.Optional;

@EventBusSubscriber(modid = ftb_subchunks.MODID)
public class SubGroupEventHandler {
    // 1. Run LAST, and make sure we can see events FTB Chunks already cancelled!
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        Boolean shouldAllow = checkOverride(player, event.getPos(), "edit");
        if (shouldAllow != null) {
            // If true, un-cancels the event. If false, cancels the event.
            event.setCanceled(!shouldAllow);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Boolean shouldAllow = checkOverride(player, event.getPos(), "edit");
        if (shouldAllow != null) {
            event.setCanceled(!shouldAllow);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Make sure this says "block_interact" now
        Boolean shouldAllow = checkOverride(player, event.getPos(), "block_interact");
        if (shouldAllow != null) {
            event.setCanceled(!shouldAllow);
        }
    }

    // NEW: Catch standard entity clicks (Animals, Villagers, Minecarts)
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Use the entity's position for the chunk check
        Boolean shouldAllow = checkOverride(player, event.getTarget().blockPosition(), "entity_interact");
        if (shouldAllow != null) {
            event.setCanceled(!shouldAllow);
        }
    }

    // NEW: Catch specific entity parts (Armor Stands, complex hitboxes)
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Boolean shouldAllow = checkOverride(player, event.getTarget().blockPosition(), "entity_interact");
        if (shouldAllow != null) {
            event.setCanceled(!shouldAllow);
        }
    }

    /**
     * Returns TRUE to force-allow, FALSE to force-deny, or NULL to let FTB Chunks decide.
     */
    private static Boolean checkOverride(ServerPlayer player, BlockPos pos, String actionType) {
        LevelChunk chunk = player.level().getChunkAt(pos);
        SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);

        // If there is no painted zone here, return null (let FTB Chunks do its normal thing)
        if (data.getZones().isEmpty()) return null;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        dev.ftb.mods.ftblibrary.math.ChunkDimPos cdp = new dev.ftb.mods.ftblibrary.math.ChunkDimPos(player.level().dimension(), chunkX, chunkZ);

        ClaimedChunk claimed = FTBChunksAPI.api().getManager().getChunk(cdp);
        if (claimed == null) return null;

        Optional<Team> ownerTeam = FTBTeamsAPI.api().getManager().getTeamByID(claimed.getTeamData().getTeam().getTeamId());
        if (ownerTeam.isEmpty()) return null;

        TeamRank playerRank = ownerTeam.get().getRankForPlayer(player.getUUID());

        for (SubZone zone : data.getZones()) {
            TeamRank requiredRank;

            // Route the action to the correct permission rank!
            switch (actionType) {
                case "edit":
                    requiredRank = zone.getBlockEditRank();
                    break;
                case "entity_interact":
                    requiredRank = zone.getEntityInteractRank();
                    break;
                case "block_interact":
                default:
                    requiredRank = zone.getBlockInteractRank();
                    break;
            }

            // Check if the player's rank meets or exceeds the required rank
            if (playerRank.isAtLeast(requiredRank)) {
                return true; // Force ALLOW
            } else {
                return false; // Force DENY
            }
        }

        return null;
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        // This fires when a chunk is loaded for a player
        LevelChunk chunk = event.getChunk();
        SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);

        if (!data.getZones().isEmpty()) {
            for (SubZone zone : data.getZones()) {
                // Tell the client to paint this chunk on their local map
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        event.getPlayer(),
                        new com.troller2705.ftb_subchunks.network.SyncSubGroupsPayload(
                                new long[]{chunk.getPos().toLong()},
                                zone.save()
                        )
                );
            }
        }
    }
}