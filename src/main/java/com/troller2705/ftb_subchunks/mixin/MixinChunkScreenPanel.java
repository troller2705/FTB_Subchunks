package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.troller2705.ftb_subchunks.client.SubGroupClientUI;
import com.troller2705.ftb_subchunks.network.SaveSubGroupPayload;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Set;

@Mixin(value = ChunkScreenPanel.class, remap = false)
public abstract class MixinChunkScreenPanel {

    @Inject(method = "mouseReleased", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"), cancellable = true)
    private void interceptPaintDrag(MouseButton button, CallbackInfo ci) {
        if (SubGroupClientUI.activePaintbrush != null && button.isLeft()) {
            ChunkScreenPanelAccessor accessor = (ChunkScreenPanelAccessor) this;
            Set<XZ> selected = accessor.getSelectedChunks();

            if (!selected.isEmpty()) {
                long[] chunkArray = new long[selected.size()];
                int i = 0;

                for (XZ xz : selected) {
                    chunkArray[i++] = ChunkPos.asLong(xz.x(), xz.z());
                }

                // FIX: Send everything to the server unconditionally.
                // The server already checks ownership, so this stops the client from accidentally blocking valid paints!
                PacketDistributor.sendToServer(new SaveSubGroupPayload(chunkArray, SubGroupClientUI.activePaintbrush.save()));

                selected.clear();
                ci.cancel();
            }
        }
    }
}