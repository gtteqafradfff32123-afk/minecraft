package com.example.titanforge;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue LIMINAL_OLD_FILM;
    public static final ForgeConfigSpec.DoubleValue FILM_GRAIN;
    public static final ForgeConfigSpec.DoubleValue FILM_VIGNETTE;
    public static final ForgeConfigSpec.DoubleValue FILM_FLICKER;
    public static final ForgeConfigSpec.DoubleValue FILM_BRIGHTNESS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("liminal_visuals");

        LIMINAL_OLD_FILM = builder
                .comment("Enable the old dark-gray film shader only inside Liminal.")
                .translation("config.titanforge.liminal_old_film")
                .define("liminalOldFilm", true);

        FILM_GRAIN = builder
                .comment("Film grain strength.")
                .translation("config.titanforge.film_grain")
                .defineInRange("filmGrain", 0.10D, 0.0D, 0.35D);

        FILM_VIGNETTE = builder
                .comment("Darkening near screen edges.")
                .translation("config.titanforge.film_vignette")
                .defineInRange("filmVignette", 0.38D, 0.0D, 0.80D);

        FILM_FLICKER = builder
                .comment("Old projector brightness flicker.")
                .translation("config.titanforge.film_flicker")
                .defineInRange("filmFlicker", 0.025D, 0.0D, 0.12D);

        FILM_BRIGHTNESS = builder
                .comment("Base brightness of the Liminal film shader.")
                .translation("config.titanforge.film_brightness")
                .defineInRange("filmBrightness", 0.76D, 0.45D, 1.0D);

        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
