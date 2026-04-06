package com.troller2705.ftb_subchunks.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class SubGroupData {
    private final List<SubZone> zones = new ArrayList<>();

    // Constructor 1: Empty
    public SubGroupData() {}

    // Constructor 2: Loading from NBT
    public SubGroupData(CompoundTag tag) {
        ListTag list = tag.getList("Zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            zones.add(new SubZone(list.getCompound(i)));
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (SubZone zone : zones) {
            list.add(zone.save());
        }
        tag.put("Zones", list);
        return tag;
    }

    public List<SubZone> getZones() { return zones; }

    public void addZone(SubZone zone) { zones.add(zone); }
}