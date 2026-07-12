package com.example.titanforge;

import com.example.titanforge.entities.GraveglassColossusEntity;
import com.example.titanforge.entities.PlagueDoctorEntity;
import com.example.titanforge.entities.PlayerCopyEntity;
import com.example.titanforge.entities.ShadowEntity;
import com.example.titanforge.entities.StunZombieEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, TitanForge.MOD_ID);

    public static final RegistryObject<EntityType<GraveglassColossusEntity>> GRAVEGLASS_COLOSSUS =
            ENTITIES.register("graveglass_colossus", () -> EntityType.Builder
                    .create(GraveglassColossusEntity::new, EntityClassification.MONSTER)
                    .size(1.15F, 3.15F)
                    .trackingRange(10)
                    .updateInterval(3)
                    .build("graveglass_colossus"));

    public static final RegistryObject<EntityType<ShadowEntity>> SHADOW =
            ENTITIES.register("shadow", () -> EntityType.Builder
                    .create(ShadowEntity::new, EntityClassification.MISC)
                    .size(0.6F, 1.8F)
                    .build("shadow"));

    public static final RegistryObject<EntityType<StunZombieEntity>> STUN_ZOMBIE =
            ENTITIES.register("stun_zombie", () -> EntityType.Builder
                    .create(StunZombieEntity::new, EntityClassification.MONSTER)
                    .size(0.6F, 1.95F)
                    .build("stun_zombie"));

    public static final RegistryObject<EntityType<PlagueDoctorEntity>> PLAGUE_DOCTOR =
            ENTITIES.register("plague_doctor", () -> EntityType.Builder
                    .create(PlagueDoctorEntity::new, EntityClassification.MONSTER)
                    .size(0.65F, 2.25F)
                    .trackingRange(8)
                    .updateInterval(3)
                    .build("plague_doctor"));

    public static final RegistryObject<EntityType<PlayerCopyEntity>> PLAYER_COPY =
            ENTITIES.register("player_copy", () -> EntityType.Builder
                    .<PlayerCopyEntity>create((type, world) -> new PlayerCopyEntity(type, world), EntityClassification.MISC)
                    .size(0.6F, 1.8F)
                    .build("player_copy"));

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(PLAGUE_DOCTOR.get(), PlagueDoctorEntity.createAttributes().create());
        event.put(GRAVEGLASS_COLOSSUS.get(), GraveglassColossusEntity.createAttributes().create());
        event.put(SHADOW.get(), ShadowEntity.registerAttributes().create());
        event.put(STUN_ZOMBIE.get(), ZombieEntity.func_234342_eQ_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 20.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.23D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 2.0D)
                .create());
        event.put(PLAYER_COPY.get(), PlayerCopyEntity.registerAttributes().create());
    }
}
