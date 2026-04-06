package com.troller2705.ftb_subchunks.mixin;

import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.troller2705.ftb_subchunks.api.ISubGroupMapChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(value = MapChunk.class, remap = false)
public abstract class MixinMapChunk implements ISubGroupMapChunk {

    @Unique
    private String subGroupName = null;

    @Unique
    private static final Map<String, Team> PROXY_CACHE = new HashMap<>();

    @Override
    public String ftbsubchunks$getSubGroupName() { return this.subGroupName; }

    @Override
    public void ftbsubchunks$setSubGroupName(String name) { this.subGroupName = name; }

    @Inject(method = "getTeam", at = @At("RETURN"), cancellable = true)
    private void overrideTeamProxy(CallbackInfoReturnable<Optional<Team>> cir) {
        if (this.subGroupName != null && cir.getReturnValue().isPresent()) {

            Team realTeam = cir.getReturnValue().get();
            final String zoneName = this.subGroupName;
            String cacheKey = realTeam.getId().toString() + "_" + zoneName;

            Team proxyTeam = PROXY_CACHE.computeIfAbsent(cacheKey, k ->
                    (Team) java.lang.reflect.Proxy.newProxyInstance(
                            Team.class.getClassLoader(),
                            new Class<?>[]{Team.class},
                            (proxy, method, args) -> {
                                String mName = method.getName();

                                // FIX: Give the Sub-Group a distinct UUID so FTB Chunks separates the map borders!
                                if (mName.equals("getId")) {
                                    return new java.util.UUID(realTeam.getId().getMostSignificantBits(), zoneName.hashCode());
                                }

                                if (mName.equals("equals") && args != null && args.length == 1) {
                                    return proxy == args[0];
                                }
                                if (mName.equals("hashCode")) {
                                    return cacheKey.hashCode();
                                }

                                if (mName.equals("getName") || mName.equals("getColoredName")) {
                                    return Component.literal(zoneName).withStyle(ChatFormatting.GOLD);
                                }
                                if (mName.equals("getShortName") || mName.equals("getStringID")) {
                                    return zoneName;
                                }

                                return method.invoke(realTeam, args);
                            }
                    )
            );

            cir.setReturnValue(Optional.of(proxyTeam));
        }
    }
}