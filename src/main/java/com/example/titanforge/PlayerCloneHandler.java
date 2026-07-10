package com.example.titanforge;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID)
public class PlayerCloneHandler {

    private static final String[] KEYS = {
        "BloodRageCharge", "BloodCharge", "BloodLastHit", "BloodCooldown",
        "BloodPactCooldown", "AgonyCooldown", "AgonyMode", "AgonyLevel", "AgonyModeEnd",
        "SingularityCooldown", "AshCooldown", "LeviathanCooldown", "PuppetCooldown",
        "PhaseRuptureCooldown", "AegisEnergy", "AegisShields"
    };

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        PlayerEntity oldP = event.getOriginal();
        PlayerEntity newP = event.getPlayer();
        CompoundNBT oldData = oldP.getPersistentData();
        CompoundNBT newData = newP.getPersistentData();

        for (String key : KEYS) {
            if (oldData.contains(key)) {
                newData.put(key, oldData.get(key).copy());
            }
        }
    }
}