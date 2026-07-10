package com.example.titanforge.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {
    public static KeyBinding ENCHANTER_KEY;

    public static void register() {
        ENCHANTER_KEY = new KeyBinding("key.titanforge.enchanter", GLFW.GLFW_KEY_G, "key.category.titanforge");
        ClientRegistry.registerKeyBinding(ENCHANTER_KEY);
    }
}
