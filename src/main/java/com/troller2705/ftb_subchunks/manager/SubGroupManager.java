package com.troller2705.ftb_subchunks.manager;

import com.troller2705.ftb_subchunks.data.SubGroupData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SubGroupManager {
    private static final SubGroupManager INSTANCE = new SubGroupManager();

    // Maps FTB Team ID to their SubGroupData
    private final Map<UUID, SubGroupData> teamDataMap = new HashMap<>();
    private File saveFile;

    public static SubGroupManager getInstance() {
        return INSTANCE;
    }

    // --- DATA ACCESS ---
    public SubGroupData getDataForTeam(UUID teamId) {
        return teamDataMap.computeIfAbsent(teamId, k -> new SubGroupData());
    }

    // --- LOADING ---
    public void load(MinecraftServer server) {
        // Creates a file at: world/ftb_subchunks_data.dat
        File dir = server.getWorldPath(LevelResource.ROOT).toFile();
        this.saveFile = new File(dir, "ftb_subchunks_data.dat");

        teamDataMap.clear();

        if (saveFile.exists()) {
            try {
                CompoundTag mainTag = NbtIo.readCompressed(saveFile.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                for (String key : mainTag.getAllKeys()) {
                    UUID teamId = UUID.fromString(key);
                    SubGroupData data = new SubGroupData(mainTag.getCompound(key));
                    teamDataMap.put(teamId, data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- SAVING ---
    public void save() {
        if (saveFile == null) return;

        CompoundTag mainTag = new CompoundTag();
        for (Map.Entry<UUID, SubGroupData> entry : teamDataMap.entrySet()) {
            mainTag.put(entry.getKey().toString(), entry.getValue().save());
        }

        try {
            NbtIo.writeCompressed(mainTag, saveFile.toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}