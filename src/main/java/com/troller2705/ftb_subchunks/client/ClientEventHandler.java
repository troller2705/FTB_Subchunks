package com.troller2705.ftb_subchunks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import com.troller2705.ftb_subchunks.ftb_subchunks;

@EventBusSubscriber(modid = ftb_subchunks.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // Wipes the palette clean when leaving a world or server
        SubGroupClientUI.BRUSH_PALETTE.clear();
        SubGroupClientUI.activePaintbrush = null;
    }
}