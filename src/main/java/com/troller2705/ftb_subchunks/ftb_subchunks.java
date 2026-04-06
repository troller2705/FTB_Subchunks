package com.troller2705.ftb_subchunks;

import com.troller2705.ftb_subchunks.events.SubGroupEventHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.troller2705.ftb_subchunks.data.SubGroupData;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ftb_subchunks.MODID)
public class ftb_subchunks
{
    public static final String MODID = "ftb_subchunks";

    public ftb_subchunks(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(SubGroupEventHandler.class);
    }
}