package com.troller2705.ftb_subchunks.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import com.troller2705.ftb_subchunks.ftb_subchunks;

public record SyncSubGroupsPayload(long[] chunks, CompoundTag zoneData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncSubGroupsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ftb_subchunks.MODID, "sync_subgroups"));

    public static final StreamCodec<FriendlyByteBuf, long[]> LONG_ARRAY_CODEC = new StreamCodec<>() {
        @Override public long[] decode(FriendlyByteBuf buf) { return buf.readLongArray(); }
        @Override public void encode(FriendlyByteBuf buf, long[] val) { buf.writeLongArray(val); }
    };

    public static final StreamCodec<FriendlyByteBuf, SyncSubGroupsPayload> STREAM_CODEC = StreamCodec.composite(
            LONG_ARRAY_CODEC, SyncSubGroupsPayload::chunks,
            ByteBufCodecs.COMPOUND_TAG, SyncSubGroupsPayload::zoneData,
            SyncSubGroupsPayload::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}