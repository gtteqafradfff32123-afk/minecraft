package com.example.titanforge.liminal.reward;

import com.example.titanforge.entities.ShadowEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

public final class DefeatedShadowTag {
    private static final String TAG_DEFEATED = "DefeatedShadow";
    private static final String TAG_OWNER = "DefeatedShadowOwner";

    public static void markDefeated(ShadowEntity shadow, UUID ownerId) {
        shadow.getPersistentData().putBoolean(TAG_DEFEATED, true);
        shadow.getPersistentData().putUniqueId(TAG_OWNER, ownerId);
    }

    public static boolean isDefeated(ShadowEntity shadow) {
        return shadow.getPersistentData().getBoolean(TAG_DEFEATED);
    }

    public static UUID getOwner(ShadowEntity shadow) {
        return shadow.getPersistentData().getUniqueId(TAG_OWNER);
    }

    public static boolean belongsTo(ShadowEntity shadow, ServerPlayerEntity player) {
        return isDefeated(shadow) && getOwner(shadow).equals(player.getUniqueID());
    }

    private DefeatedShadowTag() {}
}
