package com.troller2705.ftb_subchunks.client;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.EnumConfig;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteams.api.TeamRank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import com.troller2705.ftb_subchunks.ftb_subchunks;
import com.troller2705.ftb_subchunks.data.SubGroupData;
import com.troller2705.ftb_subchunks.data.SubZone;
import com.troller2705.ftb_subchunks.network.SaveSubGroupPayload;

import java.util.ArrayList;
import java.util.List;

public class SubGroupClientUI {

    // THE MAGIC TOGGLE STATE
    public static boolean isSubGroupMode = false;

    public static void openGUI(List<XZ> selectedChunks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // --- SAFEGUARD: Filter for Team-Owned Chunks Only ---
        List<XZ> validChunks = new ArrayList<>();
        for (XZ xz : selectedChunks) {
            // FIX 1: Pass the dimension key instead of the level object
            ChunkDimPos cdp = new ChunkDimPos(mc.level.dimension(), xz.x(), xz.z());
            ClaimedChunk claimed = FTBChunksAPI.api().getManager().getChunk(cdp);

            if (claimed != null && claimed.getTeamData() != null) {
                // FIX 2: Check membership via TeamData
                if (claimed.getTeamData().isTeamMember(mc.player.getUUID())) {
                    validChunks.add(xz);
                }
            }
        }

        if (validChunks.isEmpty()) {
            mc.player.displayClientMessage(Component.literal("§cYou can only create Sub-Groups on land claimed by your team!"), true);
            return;
        }

        // Proceed with validChunks instead of selectedChunks...
        Screen parentScreen = mc.screen;
        XZ firstChunk = validChunks.get(0);
        LevelChunk chunk = mc.level.getChunkSource().getChunkNow(firstChunk.x(), firstChunk.z());
        SubGroupData data = chunk != null ? chunk.getData(ftb_subchunks.SUBGROUP_ATTACHMENT) : new SubGroupData();

        BlockPos pos = new BlockPos(firstChunk.x() * 16, mc.player.getBlockY(), firstChunk.z() * 16);
        SubZone activeZone = data.getZones().isEmpty() ? null : data.getZones().get(0);

        if (activeZone == null) {
            activeZone = new SubZone("New Sub-Group", pos, pos.offset(15, 255, 15));
        }
        final SubZone finalZone = activeZone;

        ConfigGroup mainGroup = new ConfigGroup("ftbsubgroups.manage");
        StringConfig nameConfig = new StringConfig();
        mainGroup.add("name", nameConfig, finalZone.getName(), finalZone::setName, "New Sub-Group");

        ConfigGroup permsGroup = mainGroup.getOrCreateSubgroup("permissions");
        permsGroup.add("block_edit_rank", new EnumConfig<>(TeamRank.NAME_MAP), finalZone.getBlockEditRank(), finalZone::setBlockEditRank, TeamRank.MEMBER);
        permsGroup.add("block_interact_rank", new EnumConfig<>(TeamRank.NAME_MAP), finalZone.getBlockInteractRank(), finalZone::setBlockInteractRank, TeamRank.ALLY);
        permsGroup.add("entity_interact_rank", new EnumConfig<>(TeamRank.NAME_MAP), finalZone.getEntityInteractRank(), finalZone::setEntityInteractRank, TeamRank.ALLY);

        new EditConfigScreen(mainGroup) {
            @Override
            public void doAccept() {
                super.doAccept();

                // Pack the dragged chunks into a long array
                long[] chunkArray = new long[selectedChunks.size()];
                for (int i = 0; i < selectedChunks.size(); i++) {
                    chunkArray[i] = ChunkPos.asLong(selectedChunks.get(i).x(), selectedChunks.get(i).z());
                }

                PacketDistributor.sendToServer(new SaveSubGroupPayload(chunkArray, finalZone.save()));
                Minecraft.getInstance().setScreen(parentScreen);
            }

            @Override
            public void doCancel() {
                super.doCancel();
                Minecraft.getInstance().setScreen(parentScreen);
            }
        }.openGui();
    }
}