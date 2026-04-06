package com.troller2705.ftb_subchunks.network;

import com.troller2705.ftb_subchunks.api.ISubGroupMapChunk;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.troller2705.ftb_subchunks.ftb_subchunks;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.data.SubZone;

import java.util.ArrayList;
import java.util.List;

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

                for (long posLong : payload.chunks()) {
                    ChunkPos pos = new ChunkPos(posLong);

                    // FIX: Server-side check to prevent packet spoofing
                    dev.ftb.mods.ftbchunks.api.ClaimedChunk claimed = dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api().getManager().getChunk(new dev.ftb.mods.ftblibrary.math.ChunkDimPos(player.level().dimension(), pos.x, pos.z));

                    if (claimed != null && claimed.getTeamData() != null && claimed.getTeamData().isTeamMember(player.getUUID())) {
                        LevelChunk chunk = player.level().getChunk(pos.x, pos.z);

                        SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);
                        data.getZones().clear();
                        data.addZone(incomingZone);

                        chunk.setData(ftb_subchunks.SUBGROUP_ATTACHMENT, data);
                        chunk.setUnsaved(true);

                        successfullyPainted.add(posLong);
                    }
                }

                // Only sync the chunks that actually passed the security check
                if (!successfullyPainted.isEmpty()) {
                    long[] syncArray = new long[successfullyPainted.size()];
                    for(int i = 0; i < successfullyPainted.size(); i++) syncArray[i] = successfullyPainted.get(i);
                    PacketDistributor.sendToAllPlayers(new SyncSubGroupsPayload(syncArray, incomingZone.save()));
                }
            }
        });
    }

    @SuppressWarnings("resource")
    public static void handleSync(final SyncSubGroupsPayload payload, final net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            SubZone incomingZone = new SubZone(payload.zoneData());

            dev.ftb.mods.ftbchunks.client.map.MapManager manager = dev.ftb.mods.ftbchunks.client.map.MapManager.getInstance().orElse(null);
            if (manager == null) return;

            // Get the client player's current dimension
            var clientLevel = net.minecraft.client.Minecraft.getInstance().level;
            if (clientLevel == null) return;

            dev.ftb.mods.ftbchunks.client.map.MapDimension dimension = manager.getDimension(clientLevel.dimension());
            if (dimension == null) return;

            for (long chunkPosLong : payload.chunks()) {
                // FIX: Decode the long back into a vanilla ChunkPos to get the X and Z
                net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(chunkPosLong);

                // Use cp.x and cp.z instead of pos.x and pos.z
                dev.ftb.mods.ftbchunks.client.map.MapRegion region = dimension.getRegion(dev.ftb.mods.ftblibrary.math.XZ.regionFromChunk(cp.x, cp.z));
                if (region != null) {
                    dev.ftb.mods.ftbchunks.client.map.MapChunk mapChunk = region.getDataBlocking().getChunk(dev.ftb.mods.ftblibrary.math.XZ.of(cp.x, cp.z));

                    if (mapChunk != null) {

                        // STOP THE PALETTE LEAK
                        dev.ftb.mods.ftbteams.api.Team myTeam = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getClientManager().selfTeam();
                        if (myTeam != null && mapChunk.getTeam() != null) {
                            if (myTeam.getId().equals(mapChunk.getTeam().get().getId())) {
                                // Only learn the brush if WE own this chunk!
                                com.troller2705.ftb_subchunks.client.SubGroupClientUI.BRUSH_PALETTE.putIfAbsent(incomingZone.getName(), incomingZone);
                            }
                        }

                        // Set the map data so the Mixin can read it
                        if (mapChunk instanceof com.troller2705.ftb_subchunks.api.ISubGroupMapChunk subGroupChunk) {
                            subGroupChunk.ftbsubchunks$setSubGroupName(incomingZone.getName());
                        }

                        // FORCE VISUAL SYNC
                        region.update(true);
                    }
                }
            }
        });
    }
}