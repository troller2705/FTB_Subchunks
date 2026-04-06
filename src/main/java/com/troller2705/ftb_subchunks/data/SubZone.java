package com.troller2705.ftb_subchunks.data;

import dev.ftb.mods.ftbteams.api.TeamRank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

public class SubZone {
    private String name;
    private AABB bounds;

    // The minimum rank required to perform these actions
    private TeamRank blockEditRank = TeamRank.MEMBER;
    private TeamRank blockInteractRank = TeamRank.ALLY;
    private TeamRank entityInteractRank = TeamRank.ALLY;

    public SubZone(String name, BlockPos pos1, BlockPos pos2) {
        this.name = name;
        this.bounds = new AABB(
                pos1.getX(), pos1.getY(), pos1.getZ(),
                pos2.getX() + 1, pos2.getY() + 1, pos2.getZ() + 1
        );
    }

    public SubZone(CompoundTag tag) {
        this.name = tag.getString("Name");
        this.bounds = new AABB(
                tag.getDouble("MinX"), tag.getDouble("MinY"), tag.getDouble("MinZ"),
                tag.getDouble("MaxX"), tag.getDouble("MaxY"), tag.getDouble("MaxZ")
        );

        // FIX 1: Isolate the Try-Catches so a single failure doesn't wipe the rest
        if (tag.contains("BlockEditRank")) {
            try { this.blockEditRank = TeamRank.valueOf(tag.getString("BlockEditRank")); } catch (Exception ignored) {}
        }
        if (tag.contains("BlockInteractRank")) {
            try { this.blockInteractRank = TeamRank.valueOf(tag.getString("BlockInteractRank")); } catch (Exception ignored) {}
        }
        if (tag.contains("EntityInteractRank")) {
            try { this.entityInteractRank = TeamRank.valueOf(tag.getString("EntityInteractRank")); } catch (Exception ignored) {}
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putDouble("MinX", bounds.minX); tag.putDouble("MinY", bounds.minY); tag.putDouble("MinZ", bounds.minZ);
        tag.putDouble("MaxX", bounds.maxX); tag.putDouble("MaxY", bounds.maxY); tag.putDouble("MaxZ", bounds.maxZ);

        // FIX 2: Use .name() to ensure it matches .valueOf() perfectly upon reloading
        tag.putString("BlockEditRank", blockEditRank.name());
        tag.putString("BlockInteractRank", blockInteractRank.name());
        tag.putString("EntityInteractRank", entityInteractRank.name());

        return tag;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean contains(BlockPos pos) {
        // For the map, we only care if the X and Z are within the chunk bounds
        return pos.getX() >= bounds.minX && pos.getX() < bounds.maxX &&
                pos.getZ() >= bounds.minZ && pos.getZ() < bounds.maxZ;
    }

    // Getters & Setters for the UI
    public TeamRank getBlockEditRank() { return blockEditRank; }
    public void setBlockEditRank(TeamRank rank) { this.blockEditRank = rank; }

    public TeamRank getBlockInteractRank() { return blockInteractRank; }
    public void setBlockInteractRank(TeamRank rank) { this.blockInteractRank = rank; }

    public TeamRank getEntityInteractRank() { return entityInteractRank; }
    public void setEntityInteractRank(TeamRank rank) { this.entityInteractRank = rank; }

    // Helper methods for the Event Handler using FTB Teams 'isAtLeast' logic
    public boolean canEditBlocks(TeamRank playerRank) { return playerRank.isAtLeast(blockEditRank); }
    public boolean canInteractBlocks(TeamRank playerRank) { return playerRank.isAtLeast(blockInteractRank); }
    public boolean canInteractEntities(TeamRank playerRank) { return playerRank.isAtLeast(entityInteractRank); }
}