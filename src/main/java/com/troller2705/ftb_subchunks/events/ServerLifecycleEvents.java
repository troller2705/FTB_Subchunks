package com.troller2705.ftb_subchunks.events;

import com.troller2705.ftb_subchunks.ftb_subchunks;
import com.troller2705.ftb_subchunks.manager.SubGroupManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = ftb_subchunks.MODID)
public class ServerLifecycleEvents {

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent event) {
        // Load all team data into RAM the moment the server boots
        SubGroupManager.getInstance().load(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event) {
        // Save to disk when the server shuts down
        SubGroupManager.getInstance().save();
    }

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        // Hook into the vanilla auto-save interval (e.g., when you run /save-all)
        if (event.getLevel().getServer() != null) {
            SubGroupManager.getInstance().save();
        }
    }
}