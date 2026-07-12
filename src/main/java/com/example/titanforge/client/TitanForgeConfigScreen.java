package com.example.titanforge.client;

import com.example.titanforge.ClientConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

public final class TitanForgeConfigScreen extends Screen {
    private final Screen parent;
    private boolean closing = false;
    private boolean enabled;
    private double grain;
    private double vignette;
    private double flicker;
    private double brightness;

    public TitanForgeConfigScreen(Screen parent) {
        super(new StringTextComponent("TitanForge Config"));
        this.parent = parent;
        this.enabled = ClientConfig.LIMINAL_OLD_FILM.get();
        this.grain = ClientConfig.FILM_GRAIN.get();
        this.vignette = ClientConfig.FILM_VIGNETTE.get();
        this.flicker = ClientConfig.FILM_FLICKER.get();
        this.brightness = ClientConfig.FILM_BRIGHTNESS.get();
    }

    @Override
    protected void init() {
        int center = width / 2;
        int y = height / 2 - 88;

        addButton(new Button(center - 100, y, 200, 20,
                toggleText(), button -> {
            enabled = !enabled;
            button.setMessage(toggleText());
        }));

        addButton(new Button(center - 100, y + 26, 98, 20,
                valueText("Зерно", grain), button -> {
            grain = next(grain, 0.05D, 0.0D, 0.35D);
            button.setMessage(valueText("Зерно", grain));
        }));

        addButton(new Button(center + 2, y + 26, 98, 20,
                valueText("Виньетка", vignette), button -> {
            vignette = next(vignette, 0.10D, 0.0D, 0.80D);
            button.setMessage(valueText("Виньетка", vignette));
        }));

        addButton(new Button(center - 100, y + 52, 98, 20,
                valueText("Мерцание", flicker), button -> {
            flicker = next(flicker, 0.01D, 0.0D, 0.12D);
            button.setMessage(valueText("Мерцание", flicker));
        }));

        addButton(new Button(center + 2, y + 52, 98, 20,
                valueText("Яркость", brightness), button -> {
            brightness = next(brightness, 0.05D, 0.45D, 1.0D);
            button.setMessage(valueText("Яркость", brightness));
        }));

        addButton(new Button(center - 100, y + 88, 98, 20,
                new StringTextComponent("Сохранить"), button -> saveAndClose()));

        addButton(new Button(center + 2, y + 88, 98, 20,
                new StringTextComponent("Отмена"), button -> onClose()));
    }

    private StringTextComponent toggleText() {
        return new StringTextComponent(
                "Старое кино: " + (enabled ? "ВКЛ" : "ВЫКЛ"));
    }

    private static StringTextComponent valueText(String name, double value) {
        return new StringTextComponent(
                name + ": " + Math.round(value * 100.0D) + "%");
    }

    private static double next(double value, double step,
                               double min, double max) {
        double result = value + step;
        if (result > max + 0.0001D) result = min;
        return Math.round(result * 100.0D) / 100.0D;
    }

    private void saveAndClose() {
        ClientConfig.LIMINAL_OLD_FILM.set(enabled);
        ClientConfig.FILM_GRAIN.set(grain);
        ClientConfig.FILM_VIGNETTE.set(vignette);
        ClientConfig.FILM_FLICKER.set(flicker);
        ClientConfig.FILM_BRIGHTNESS.set(brightness);
        ClientConfig.SPEC.save();

        LiminalFilmController.refreshNow();
        minecraft.displayGuiScreen(parent);
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        minecraft.displayGuiScreen(parent);
    }

    @Override
    public void render(com.mojang.blaze3d.matrix.MatrixStack stack,
                       int mouseX, int mouseY, float partialTicks) {
        renderBackground(stack);
        drawCenteredString(stack, font, title, width / 2,
                height / 2 - 122, 0xD8D8D8);
        drawCenteredString(stack, font,
                "Фильтр работает только внутри Liminal",
                width / 2, height / 2 - 106, 0x8F8F8F);
        super.render(stack, mouseX, mouseY, partialTicks);
    }
}
