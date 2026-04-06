package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.troller2705.ftb_subchunks.client.SubGroupClientUI;

import java.util.ArrayList;
import java.util.Set;

@Mixin(value = ChunkScreenPanel.class, remap = false)
public abstract class MixinChunkScreenPanel {

    @Inject(method = "mouseReleased", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"), cancellable = true)
    private void interceptClaimDrag(MouseButton button, CallbackInfo ci) {
        if (SubGroupClientUI.isSubGroupMode) {
            ChunkScreenPanelAccessor accessor = (ChunkScreenPanelAccessor) this;
            Set<XZ> selected = accessor.getSelectedChunks();

            if (!selected.isEmpty()) {
                // Open our UI with the dragged chunks
                SubGroupClientUI.openGUI(new ArrayList<>(selected));

                // Clear the selection so FTB Chunks doesn't try to claim/unclaim them
                selected.clear();

                // Cancel the original method so it doesn't process the (now empty) list
                ci.cancel();
            }
        }
    }
}