package com.troller2705.ftb_subchunks.network;

import com.troller2705.ftb_subchunks.api.ISubGroupMapChunk;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.troller2705.ftb_subchunks.ftb_subchunks;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.data.SubZone;
import com.troller2705.ftb_subchunks.manager.SubGroupManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ftb_subchunks.MODID)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToServer(SaveSubGroupPayload.TYPE, SaveSubGroupPayload.STREAM_CODEC, NetworkHandler::handleSave);
        registrar.playToClient(SyncSubGroupsPayload.TYPE, SyncSubGroupsPayload.STREAM_CODEC, NetworkHandler::handleSync);
    }

    @SuppressWarnings("resource")
    private static void handleSave(final SaveSubGroupPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SubZone incomingZone = new SubZone(payload.zoneData());
                List<Long> successfullyPainted = new ArrayList<>();

                dev.ftb.mods.ftbteams.api.Team team = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getManager().getTeamForPlayer(player).orElse(null);

                if (team != null) {
                    SubGroupData teamData = SubGroupManager.getInstance().getDataForTeam(team.getId());

                    // --- 1. THE CASCADE SYNC ---
                    // Find every chunk that already has this SubGroup and overwrite it with the new permissions!
                    boolean globalUpdate = false;
                    for (Map.Entry<Long, SubZone> entry : teamData.getChunkZones().entrySet()) {
                        if (entry.getValue().getName().equals(incomingZone.getName())) {
                            entry.setValue(new SubZone(payload.zoneData()));

                            // Add it to the array so the client map redraws it!
                            if (!successfullyPainted.contains(entry.getKey())) {
                                successfullyPainted.add(entry.getKey());
                            }
                            globalUpdate = true;
                        }
                    }

                    // --- 2. ADD BRAND NEW PAINTED CHUNKS ---
                    for (long posLong : payload.chunks()) {
                        ChunkPos pos = new ChunkPos(posLong);
                        dev.ftb.mods.ftbchunks.api.ClaimedChunk claimed = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api().getManager().getChunk(new dev.ftb.mods.ftblibrary.math.ChunkDimPos(player.level().dimension(), pos.x, pos.z));

                        if (claimed != null && claimed.getTeamData() != null && claimed.getTeamData().isTeamMember(player.getUUID())) {
                            teamData.setChunkZone(posLong, incomingZone);
                            if (!successfullyPainted.contains(posLong)) {
                                successfullyPainted.add(posLong);
                            }
                        }
                    }

                    // --- 3. SEND THE UPDATE TO EVERYONE ---
                    if (!successfullyPainted.isEmpty() || globalUpdate) {
                        SubGroupManager.getInstance().save();

                        long[] syncArray = new long[successfullyPainted.size()];
                        for(int i = 0; i < successfullyPainted.size(); i++) syncArray[i] = successfullyPainted.get(i);

                        net.minecraft.nbt.CompoundTag tag = incomingZone.save();
                        tag.putString("OwnerTeamId", team.getId().toString());

                        PacketDistributor.sendToAllPlayers(new SyncSubGroupsPayload(syncArray, tag));
                    }
                }
            }
        });
    }

    @SuppressWarnings("resource")
    private static void handleSync(final SyncSubGroupsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            SubZone incomingZone = new SubZone(payload.zoneData());
            String ownerId = payload.zoneData().getString("OwnerTeamId");
            dev.ftb.mods.ftbteams.api.Team myTeam = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getClientManager().selfTeam();

            // Secure Palette check: Only add the brush if we own it
            if (myTeam != null && !ownerId.isEmpty() && myTeam.getId().toString().equals(ownerId)) {
                com.troller2705.ftb_subchunks.client.SubGroupClientUI.BRUSH_PALETTE.putIfAbsent(incomingZone.getName(), incomingZone);
            }

            dev.ftb.mods.ftbchunks.client.map.MapManager manager = dev.ftb.mods.ftbchunks.client.map.MapManager.getInstance().orElse(null);
            if (manager == null) return;

            dev.ftb.mods.ftbchunks.client.map.MapDimension dimension = manager.getDimension(mc.level.dimension());
            if (dimension == null) return;

            for (long posLong : payload.chunks()) {
                net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(posLong);
                dev.ftb.mods.ftbchunks.client.map.MapRegion region = dimension.getRegion(dev.ftb.mods.ftblibrary.math.XZ.regionFromChunk(pos.x, pos.z));

                if (region != null) {
                    dev.ftb.mods.ftbchunks.client.map.MapChunk mapChunk = region.getDataBlocking().getChunk(dev.ftb.mods.ftblibrary.math.XZ.of(pos.x, pos.z));

                    if (mapChunk != null && mapChunk instanceof ISubGroupMapChunk subGroupChunk) {
                        subGroupChunk.ftbsubchunks$setSubGroupName(incomingZone.getName());
                        region.update(true); // Redraw map texture
                    }
                }
            }

            if (mc.screen instanceof dev.ftb.mods.ftblibrary.ui.ScreenWrapper sw) {
                if (sw.getGui() instanceof dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen lms) {
                    lms.refreshWidgets();
                }
            }
        });
    }
}