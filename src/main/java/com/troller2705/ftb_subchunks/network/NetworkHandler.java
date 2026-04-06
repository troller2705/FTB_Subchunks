package com.troller2705.ftb_subchunks.network;

import net.minecraft.client.Minecraft;
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

                for (long posLong : payload.chunks()) {
                    ChunkPos pos = new ChunkPos(posLong);
                    LevelChunk chunk = player.level().getChunk(pos.x, pos.z);

                    // NeoForge guarantees this is never null!
                    SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);

                    data.getZones().removeIf(z -> z.getName().equals(incomingZone.getName()));
                    data.addZone(incomingZone);
                    chunk.setUnsaved(true); // Tell NeoForge to save to disk
                }

                // Immediately broadcast the change so clients map's update
                PacketDistributor.sendToAllPlayers(new SyncSubGroupsPayload(payload.chunks(), incomingZone.save()));
            }
        });
    }

    @SuppressWarnings("resource")
    private static void handleSync(final SyncSubGroupsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            SubZone incomingZone = new SubZone(payload.zoneData());
            for (long posLong : payload.chunks()) {
                ChunkPos pos = new ChunkPos(posLong);
                LevelChunk chunk = mc.level.getChunkSource().getChunkNow(pos.x, pos.z);

                if (chunk != null) {
                    // FORCE THE UPDATE: Get existing or new, update it, then RE-SET it
                    SubGroupData data = chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT);
                    data.getZones().removeIf(z -> z.getName().equals(incomingZone.getName()));
                    data.addZone(incomingZone);

                    // Explicitly set it again to trigger any internal listeners
                    chunk.setData(ftb_subchunks.SUBGROUP_ATTACHMENT, data);
                }
            }

            // OPTIONAL: Force the map to refresh icons/tooltips immediately
            if (mc.screen instanceof dev.ftb.mods.ftblibrary.ui.ScreenWrapper sw) {
                if (sw.getGui() instanceof dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen lms) {
                    lms.refreshWidgets();
                }
            }
        });
    }
}