package com.example.titanforge.client.renderer;

import com.example.titanforge.TitanForge;
import com.example.titanforge.client.model.PlagueDoctorModel;
import com.example.titanforge.entities.PlagueDoctorEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;

public final class PlagueDoctorRenderer
        extends MobRenderer<PlagueDoctorEntity, PlagueDoctorModel<PlagueDoctorEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            TitanForge.MOD_ID, "textures/entity/plague_doctor.png");

    public PlagueDoctorRenderer(EntityRendererManager manager) {
        super(manager, new PlagueDoctorModel<>(), 0.55F);
    }

    @Override
    public ResourceLocation getEntityTexture(PlagueDoctorEntity entity) {
        return TEXTURE;
    }
}
