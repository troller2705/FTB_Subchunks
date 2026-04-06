package com.troller2705.ftb_subchunks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.troller2705.ftb_subchunks.ftb_subchunks;

public record OpenSubGroupGuiPayload(BlockPos pos) implements CustomPacketPayload {
    // Unique ID for this packet type
    public static final CustomPacketPayload.Type<OpenSubGroupGuiPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ftb_subchunks.MODID, "open_subgroup_gui"));

    // Codec that automatically serializes/deserializes the BlockPos
    public static final StreamCodec<FriendlyByteBuf, OpenSubGroupGuiPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenSubGroupGuiPayload::pos,
            OpenSubGroupGuiPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}