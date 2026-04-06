package com.troller2705.ftb_subchunks.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import com.troller2705.ftb_subchunks.ftb_subchunks;

public record SaveSubGroupPayload(long[] chunks, CompoundTag zoneData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveSubGroupPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ftb_subchunks.MODID, "save_subgroup"));

    // A foolproof, custom codec for sending long arrays over the network
    public static final StreamCodec<FriendlyByteBuf, long[]> LONG_ARRAY_CODEC = new StreamCodec<>() {
        @Override public long[] decode(FriendlyByteBuf buf) { return buf.readLongArray(); }
        @Override public void encode(FriendlyByteBuf buf, long[] val) { buf.writeLongArray(val); }
    };

    public static final StreamCodec<FriendlyByteBuf, SaveSubGroupPayload> STREAM_CODEC = StreamCodec.composite(
            LONG_ARRAY_CODEC, SaveSubGroupPayload::chunks,
            ByteBufCodecs.COMPOUND_TAG, SaveSubGroupPayload::zoneData,
            SaveSubGroupPayload::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}