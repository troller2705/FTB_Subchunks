package com.troller2705.ftb_subchunks.client;

import dev.ftb.mods.ftblibrary.config.BooleanConfig;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.EnumConfig;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftbteams.api.TeamRank;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor; // NEW IMPORT
import com.troller2705.ftb_subchunks.network.SaveSubGroupPayload; // NEW IMPORT
import com.troller2705.ftb_subchunks.data.SubZone;

import java.util.HashMap;
import java.util.Map;

public class SubGroupClientUI {

    public static SubZone activePaintbrush = null;
    public static final Map<String, SubZone> BRUSH_PALETTE = new HashMap<>();

    public static void openPaletteMenu(Runnable returnToMap) {
        new SubGroupListScreen(returnToMap).openGui();
    }

    /**
     * Opens the editor for a specific Sub-Group.
     * @param returnToList The screen to go back to (the List Screen)
     * @param returnToMap  The action to perform to return to the actual Chunk Map
     */
    public static void openBrushEditor(SubZone zone, BaseScreen returnToList, Runnable returnToMap) {
        String originalName = zone.getName();
        ConfigGroup mainGroup = new ConfigGroup("ftbsubchunks.brush_edit");

        // The "Equip" toggle
        final boolean[] equip = {false};
        mainGroup.add("equip_brush", new BooleanConfig(), false, v -> equip[0] = v, false)
                .setNameKey("Equip this Brush?");

        // Permission settings
        mainGroup.add("name", new StringConfig(), zone.getName(), zone::setName, "New Sub-Group");

        ConfigGroup permsGroup = mainGroup.getOrCreateSubgroup("permissions");
        permsGroup.add("block_edit_rank", new EnumConfig<>(TeamRank.NAME_MAP), zone.getBlockEditRank(), zone::setBlockEditRank, TeamRank.MEMBER);
        permsGroup.add("block_interact_rank", new EnumConfig<>(TeamRank.NAME_MAP), zone.getBlockInteractRank(), zone::setBlockInteractRank, TeamRank.ALLY);
        permsGroup.add("entity_interact_rank", new EnumConfig<>(TeamRank.NAME_MAP), zone.getEntityInteractRank(), zone::setEntityInteractRank, TeamRank.ALLY);

        // EditConfigScreen creates a standard FTB property-style menu
        new EditConfigScreen(mainGroup) {
            @Override
            public void doAccept() {
                super.doAccept();

                // Keep the palette updated
                if (!originalName.equals(zone.getName())) {
                    BRUSH_PALETTE.remove(originalName);
                }
                BRUSH_PALETTE.put(zone.getName(), zone);

                // --- CASCADE SYNC TRIGGER ---
                // Send the updated brush to the server to cascade the new permissions to all painted chunks!
                PacketDistributor.sendToServer(new SaveSubGroupPayload(new long[0], zone.save()));

                if (equip[0]) {
                    // Equip the brush and jump straight back to the map to start painting
                    activePaintbrush = new SubZone(zone.getName(), BlockPos.ZERO, BlockPos.ZERO);
                    activePaintbrush.setBlockEditRank(zone.getBlockEditRank());
                    activePaintbrush.setBlockInteractRank(zone.getBlockInteractRank());
                    activePaintbrush.setEntityInteractRank(zone.getEntityInteractRank());

                    Minecraft.getInstance().player.displayClientMessage(Component.literal("§e[Sub-Chunks] §aPaintbrush equipped: §6" + activePaintbrush.getName()), false);
                    returnToMap.run();
                } else {
                    // Just save and go back to the selection list
                    returnToList.openGui();
                }
            }

            @Override
            public void doCancel() {
                super.doCancel();
                returnToList.openGui();
            }
        }.openGui();
    }
}