package com.troller2705.ftb_subchunks.client;

import com.troller2705.ftb_subchunks.api.ISubGroupMapChunk;
import net.minecraft.world.entity.player.Player;

public class ClientSubGroupHelper {
    public static Boolean hasSubGroup(Player player, int chunkX, int chunkZ) {
        // Safely ask the Client Map if this chunk has a SubGroup painted on it
        dev.ftb.mods.ftbchunks.client.map.MapManager manager = dev.ftb.mods.ftbchunks.client.map.MapManager.getInstance().orElse(null);
        if (manager == null) return null;

        dev.ftb.mods.ftbchunks.client.map.MapDimension dim = manager.getDimension(player.level().dimension());
        if (dim == null) return null;

        dev.ftb.mods.ftbchunks.client.map.MapRegion region = dim.getRegion(dev.ftb.mods.ftblibrary.math.XZ.regionFromChunk(chunkX, chunkZ));
        if (region == null) return null;

        dev.ftb.mods.ftbchunks.client.map.MapChunk mapChunk = region.getDataBlocking().getChunk(dev.ftb.mods.ftblibrary.math.XZ.of(chunkX, chunkZ));
        if (mapChunk instanceof ISubGroupMapChunk subGroupChunk) {
            String zoneName = subGroupChunk.ftbsubchunks$getSubGroupName();
            if (zoneName != null && !zoneName.isEmpty()) {
                // We don't know the exact permissions on the client, but we know a SubGroup exists.
                // We force-allow the client so it successfully sends the packet to the Server!
                return true;
            }
        }
        return null;
    }
}