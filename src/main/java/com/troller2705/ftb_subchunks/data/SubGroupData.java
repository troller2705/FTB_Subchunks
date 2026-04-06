package com.troller2705.ftb_subchunks.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.HashMap;
import java.util.Map;

public class SubGroupData {
    // Maps a vanilla ChunkPos (long) to its assigned SubZone
    private final Map<Long, SubZone> chunkZones = new HashMap<>();

    public SubGroupData() {}

    public SubGroupData(CompoundTag tag) {
        ListTag list = tag.getList("ChunkZones", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long pos = entry.getLong("ChunkPos");
            SubZone zone = new SubZone(entry.getCompound("ZoneData"));
            chunkZones.put(pos, zone);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<Long, SubZone> entry : chunkZones.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("ChunkPos", entry.getKey());
            entryTag.put("ZoneData", entry.getValue().save());
            list.add(entryTag);
        }
        tag.put("ChunkZones", list);
        return tag;
    }

    public SubZone getZoneForChunk(long chunkPos) {
        return chunkZones.get(chunkPos);
    }

    public void setChunkZone(long chunkPos, SubZone zone) {
        chunkZones.put(chunkPos, zone);
    }

    public Map<Long, SubZone> getChunkZones() {
        return chunkZones;
    }
}