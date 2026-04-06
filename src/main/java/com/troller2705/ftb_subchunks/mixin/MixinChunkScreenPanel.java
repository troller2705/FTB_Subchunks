package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.troller2705.ftb_subchunks.client.SubGroupClientUI;
import com.troller2705.ftb_subchunks.network.SaveSubGroupPayload;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(value = ChunkScreenPanel.class, remap = false)
public abstract class MixinChunkScreenPanel {

    @Inject(method = "mouseReleased", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"), cancellable = true)
    private void interceptPaintDrag(MouseButton button, CallbackInfo ci) {
        if (SubGroupClientUI.activePaintbrush != null && button.isLeft()) {

            ChunkScreenPanelAccessor accessor = (ChunkScreenPanelAccessor) this;
            Set<XZ> selected = accessor.getSelectedChunks();

            if (!selected.isEmpty()) {
                Minecraft mc = Minecraft.getInstance();
                List<Long> validChunks = new ArrayList<>();

                // --- NEW CLIENT-SAFE LOGIC ---
                // Grab the client's local cache of their Team and the Map
                dev.ftb.mods.ftbteams.api.Team myTeam = dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api().getClientManager().selfTeam();
                dev.ftb.mods.ftbchunks.client.map.MapManager manager = dev.ftb.mods.ftbchunks.client.map.MapManager.getInstance().orElse(null);

                if (myTeam != null && manager != null && mc.level != null) {
                    dev.ftb.mods.ftbchunks.client.map.MapDimension dimension = manager.getDimension(mc.level.dimension());

                    if (dimension != null) {
                        for (XZ xz : selected) {
                            dev.ftb.mods.ftbchunks.client.map.MapRegion region = dimension.getRegion(XZ.regionFromChunk(xz.x(), xz.z()));
                            if (region != null) {
                                // Retrieve the visual map chunk data
                                dev.ftb.mods.ftbchunks.client.map.MapChunk mapChunk = region.getDataBlocking().getChunk(xz);

                                // Verify the map chunk has our team ID attached to it before painting!
                                if (mapChunk != null && mapChunk.getTeam() != null) {
                                    if (mapChunk.getTeam().get().getTeamId().equals(myTeam.getId())) {
                                        validChunks.add(ChunkPos.asLong(xz.x(), xz.z()));
                                    }
                                }
                            }
                        }
                    }
                }
                // --- END CLIENT-SAFE LOGIC ---

                if (!validChunks.isEmpty()) {
                    long[] chunkArray = new long[validChunks.size()];
                    for (int i = 0; i < validChunks.size(); i++) {
                        chunkArray[i] = validChunks.get(i);
                    }

                    PacketDistributor.sendToServer(new SaveSubGroupPayload(chunkArray, SubGroupClientUI.activePaintbrush.save()));
                }

                selected.clear();
                ci.cancel();
            }
        }
    }
}