package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.troller2705.ftb_subchunks.client.SubGroupClientUI;
import com.troller2705.ftb_subchunks.data.SubZone;
import com.troller2705.ftb_subchunks.network.SaveSubGroupPayload;
import net.minecraft.core.BlockPos;
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

                // FIX: Only apply the Paintbrush to chunks your team owns
                for (XZ xz : selected) {
                    ChunkDimPos cdp = new ChunkDimPos(mc.level.dimension(), xz.x(), xz.z());
                    ClaimedChunk claimed = FTBChunksAPI.api().getManager().getChunk(cdp);

                    if (claimed != null && claimed.getTeamData() != null && claimed.getTeamData().isTeamMember(mc.player.getUUID())) {
                        validChunks.add(ChunkPos.asLong(xz.x(), xz.z()));
                    }
                }


                if (!validChunks.isEmpty()) {
                    long[] chunkArray = new long[validChunks.size()];
                    for (int i = 0; i < validChunks.size(); i++) {
                        chunkArray[i] = validChunks.get(i);
                    }

                    // FIX: Send the FULL activePaintbrush (with its permissions) to the server!
                    PacketDistributor.sendToServer(new SaveSubGroupPayload(chunkArray, SubGroupClientUI.activePaintbrush.save()));
                }

                selected.clear();
                ci.cancel();
            }
        }
    }
}