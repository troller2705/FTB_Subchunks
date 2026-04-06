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

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final Supplier<AttachmentType<SubGroupData>> SUBGROUP_ATTACHMENT = ATTACHMENT_TYPES.register(
            "subgroup_data",
            () -> AttachmentType.builder(() -> new SubGroupData())
                    .serialize(new IAttachmentSerializer<CompoundTag, SubGroupData>() {
                        @Override
                        public SubGroupData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            return new SubGroupData(tag);
                        }

                        @Override
                        public CompoundTag write(SubGroupData attachment, HolderLookup.Provider provider) {
                            return attachment.save();
                        }
                    })
                    .build() // <--- Cleaned up!
    );

    public ftb_subchunks(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.register(SubGroupEventHandler.class);
    }
}