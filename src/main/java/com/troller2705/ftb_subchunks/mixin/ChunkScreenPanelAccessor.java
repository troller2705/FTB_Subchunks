package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel;
import dev.ftb.mods.ftblibrary.math.XZ;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(value = ChunkScreenPanel.class, remap = false)
public interface ChunkScreenPanelAccessor {
    @Accessor("selectedChunks")
    Set<XZ> getSelectedChunks();
}