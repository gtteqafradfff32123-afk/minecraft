package com.example.titanforge;

import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

/**
 * Видимый эффект болезни: уровень I/II/III = стадия вируса.
 * Вся логика в ZombieVirusHandler, эффект — индикатор с иконкой
 * (и позволяет заражать через /effect give).
 */
public class ZombieVirusEffect extends Effect {
    public ZombieVirusEffect() {
        super(EffectType.HARMFUL, 0x6BBF3F);
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
    }
}
