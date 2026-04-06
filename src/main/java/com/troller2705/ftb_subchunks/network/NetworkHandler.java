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
    private static void handleSync(final SyncSubGroupsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            SubZone incomingZone = new SubZone(payload.zoneData());

            // NEW: Automatically add this brush to the Client Palette so they can equip it later!
            com.troller2705.ftb_subchunks.client.SubGroupClientUI.BRUSH_PALETTE.putIfAbsent(incomingZone.getName(), incomingZone);

            dev.ftb.mods.ftbchunks.client.map.MapManager manager = dev.ftb.mods.ftbchunks.client.map.MapManager.getInstance().orElse(null);
            if (manager == null) return;

            dev.ftb.mods.ftbchunks.client.map.MapDimension dimension = manager.getDimension(mc.level.dimension());
            if (dimension == null) return;

            for (long posLong : payload.chunks()) {
                net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(posLong);

                // 1. Get FTB's exact MapChunk
                dev.ftb.mods.ftbchunks.client.map.MapChunk mapChunk = dimension.getRegion(dev.ftb.mods.ftblibrary.math.XZ.regionFromChunk(pos.x, pos.z))
                        .getDataBlocking().getChunk(dev.ftb.mods.ftblibrary.math.XZ.of(pos.x, pos.z));

                if (mapChunk != null) {
                    // 2. Safely cast it to our Duck-Typed interface and set the native data!
                    if (mapChunk instanceof ISubGroupMapChunk subGroupChunk) {
                        subGroupChunk.ftbsubchunks$setSubGroupName(incomingZone.getName());
                    }
                }
            }

            // Force redraw the Large Map if it is open
            if (mc.screen instanceof dev.ftb.mods.ftblibrary.ui.ScreenWrapper sw) {
                if (sw.getGui() instanceof dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen lms) {
                    lms.refreshWidgets();
                }
            }
        });
    }
}