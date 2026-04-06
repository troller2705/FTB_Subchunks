package com.troller2705.ftb_subchunks.events;

import com.troller2705.ftb_subchunks.ftb_subchunks;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.data.SubZone;
import com.troller2705.ftb_subchunks.manager.SubGroupManager;
import com.troller2705.ftb_subchunks.network.SyncSubGroupsPayload;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

@EventBusSubscriber(modid = ftb_subchunks.MODID)
public class SubGroupEventHandler {

    // --- SYNC EVENTS ---
    @SubscribeEvent
    public static void onPlayerJoin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (ClaimedChunk claimedChunk : FTBChunksAPI.api().getManager().getAllClaimedChunks()) {
                java.util.UUID ownerTeamId = claimedChunk.getTeamData().getTeam().getTeamId();
                SubGroupData data = SubGroupManager.getInstance().getDataForTeam(ownerTeamId);
                long chunkPosLong = claimedChunk.getPos().chunkPos().toLong();
                SubZone zone = data.getZoneForChunk(chunkPosLong);

                if (zone != null) {
                    CompoundTag tag = zone.save();
                    tag.putString("OwnerTeamId", ownerTeamId.toString());
                    PacketDistributor.sendToPlayer(player, new SyncSubGroupsPayload(new long[]{chunkPosLong}, tag));
                }
            }
        }
    }

    // --- PERMISSION EVENTS (Dual-Sided, LOWEST Priority) ---

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Boolean shouldAllow = checkOverride(event.getPlayer(), event.getPos(), "edit");
        applyOverride(event, event.getPlayer(), shouldAllow); // BreakEvent uses getPlayer()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Boolean shouldAllow = checkOverride(event.getEntity(), event.getPos(), "edit");
        applyOverride(event, event.getEntity(), shouldAllow); // InteractEvents use getEntity()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // EntityPlaceEvent could be a non-player entity (like a dispenser), so we must check!
        if (event.getEntity() instanceof Player player) {
            Boolean shouldAllow = checkOverride(player, event.getPos(), "edit");
            applyOverride(event, player, shouldAllow);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        Boolean shouldAllow = checkOverride(event.getEntity(), event.getPos(), "block_interact");
        applyOverride(event, event.getEntity(), shouldAllow);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Boolean shouldAllow = checkOverride(event.getEntity(), event.getTarget().blockPosition(), "entity_interact");
        applyOverride(event, event.getEntity(), shouldAllow);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Boolean shouldAllow = checkOverride(event.getEntity(), event.getTarget().blockPosition(), "entity_interact");
        applyOverride(event, event.getEntity(), shouldAllow);
    }

    // --- THE FORCE-ALLOW SCRUBBER ---
    private static void applyOverride(ICancellableEvent event, Player player, Boolean shouldAllow) {
        if (shouldAllow != null) {
            if (shouldAllow) {
                event.setCanceled(false);

                // --- WIPE THE FTB CHUNKS ACTION BAR TEXT ---
                // "true" sends the message to the Action Bar (above the hotbar) instead of the chat log
                player.displayClientMessage(net.minecraft.network.chat.Component.empty(), true);

                if (event instanceof PlayerInteractEvent.RightClickBlock rcb) {
                    rcb.setUseBlock(net.neoforged.neoforge.common.util.TriState.DEFAULT);
                    rcb.setUseItem(net.neoforged.neoforge.common.util.TriState.DEFAULT);
                } else if (event instanceof PlayerInteractEvent.LeftClickBlock lcb) {
                    lcb.setUseBlock(net.neoforged.neoforge.common.util.TriState.DEFAULT);
                    lcb.setUseItem(net.neoforged.neoforge.common.util.TriState.DEFAULT);
                }
            } else {
                event.setCanceled(true);

                if (event instanceof PlayerInteractEvent.RightClickBlock rcb) {
                    rcb.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
                    rcb.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
                } else if (event instanceof PlayerInteractEvent.LeftClickBlock lcb) {
                    lcb.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
                    lcb.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
                }
            }
        }
    }


    // --- CORE DUAL-SIDED LOGIC ---
    private static Boolean checkOverride(Player player, BlockPos pos, String actionType) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        if (player.level().isClientSide()) {
            // --- CLIENT SIDE CHECK ---
            return com.troller2705.ftb_subchunks.client.ClientSubGroupHelper.hasSubGroup(player, chunkX, chunkZ);

        } else if (player instanceof ServerPlayer serverPlayer) {
            // --- SERVER SIDE CHECK ---
            dev.ftb.mods.ftblibrary.math.ChunkDimPos cdp = new dev.ftb.mods.ftblibrary.math.ChunkDimPos(serverPlayer.level().dimension(), chunkX, chunkZ);
            ClaimedChunk claimed = FTBChunksAPI.api().getManager().getChunk(cdp);
            if (claimed == null) return null;

            SubGroupData data = SubGroupManager.getInstance().getDataForTeam(claimed.getTeamData().getTeam().getTeamId());
            long chunkPosLong = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ).toLong();
            SubZone zone = data.getZoneForChunk(chunkPosLong);

            if (zone == null) return null;

            Optional<Team> ownerTeam = FTBTeamsAPI.api().getManager().getTeamByID(claimed.getTeamData().getTeam().getTeamId());
            if (ownerTeam.isEmpty()) return null;

            TeamRank playerRank = ownerTeam.get().getRankForPlayer(serverPlayer.getUUID());
            TeamRank requiredRank;

            switch (actionType) {
                case "edit": requiredRank = zone.getBlockEditRank(); break;
                case "entity_interact": requiredRank = zone.getEntityInteractRank(); break;
                case "block_interact":
                default: requiredRank = zone.getBlockInteractRank(); break;
            }

            // Use FTB's raw power integers to bypass their inverted isAtLeast logic!
            return playerRank.getPower() >= requiredRank.getPower();
        }
        return null;
    }
}