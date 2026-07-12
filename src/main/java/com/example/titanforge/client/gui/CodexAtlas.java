package com.example.titanforge.client.gui;

import com.example.titanforge.TitanForge;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;

public final class CodexAtlas {
    public static final int TEXTURE_W = 512;
    public static final int TEXTURE_H = 256;
    public static final int GUI_W = 320;
    public static final int GUI_H = 220;

    public static final ResourceLocation TEXTURE = new ResourceLocation(
        TitanForge.MOD_ID,
        "textures/gui/titanforge_codex_atlas.png"
    );

    private CodexAtlas() {}

    public static void bind() {
        Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
    }

    public static void sprite(MatrixStack stack, int x, int y,
                              int u, int v, int width, int height) {
        AbstractGui.blit(
            stack,
            x, y,
            (float) u, (float) v,
            width, height,
            TEXTURE_W, TEXTURE_H
        );
    }

    public static void base(MatrixStack stack, int x, int y) {
        bind();
        sprite(stack, x, y, 0, 0, GUI_W, GUI_H);
    }

    public enum TabState {
        NORMAL(328, 8),
        ACTIVE(380, 8),
        HOVER(432, 8);

        public final int u;
        public final int v;
        TabState(int u, int v) { this.u = u; this.v = v; }
    }

    public static void tab(MatrixStack stack, int x, int y,
                           int width, TabState state) {
        bind();
        sprite(stack, x, y, state.u, state.v, Math.min(width, 48), 16);
    }

    public enum SearchState {
        NORMAL(328, 30),
        FOCUSED(328, 48);

        public final int u;
        public final int v;
        SearchState(int u, int v) { this.u = u; this.v = v; }
    }

    public static void search(MatrixStack stack, int x, int y, SearchState state) {
        bind();
        sprite(stack, x, y, state.u, state.v, 108, 14);
    }

    public enum RowState {
        NORMAL(328, 70),
        HOVER(328, 87),
        SELECTED(328, 104),
        FORBIDDEN(328, 121);

        public final int u;
        public final int v;
        RowState(int u, int v) { this.u = u; this.v = v; }
    }

    public static void row(MatrixStack stack, int x, int y, RowState state) {
        bind();
        sprite(stack, x, y, state.u, state.v, 108, 13);
    }

    public static void chip(MatrixStack stack, int x, int y, boolean accent) {
        bind();
        sprite(stack, x, y, accent ? 388 : 328, 141, 56, 15);
    }

    public static void navLeft(MatrixStack stack, int x, int y) {
        bind();
        sprite(stack, x, y, 328, 164, 22, 18);
    }

    public static void navRight(MatrixStack stack, int x, int y) {
        bind();
        sprite(stack, x, y, 354, 164, 22, 18);
    }

    public static void activation(MatrixStack stack, int x, int y) {
        bind();
        sprite(stack, x, y, 328, 188, 152, 34);
    }

    public enum DiamondState {
        RARE(442, 28),
        MYTHIC(472, 28),
        FORBIDDEN(442, 60),
        GOLD(472, 60);

        public final int u;
        public final int v;
        DiamondState(int u, int v) { this.u = u; this.v = v; }
    }

    public static void diamond(MatrixStack stack, int x, int y, DiamondState state) {
        bind();
        sprite(stack, x, y, state.u, state.v, 28, 28);
    }

    public enum ScrollState {
        TOP(448, 104),
        MIDDLE(456, 114),
        BOTTOM(464, 124),
        DISABLED(472, 104);

        public final int u;
        public final int v;
        ScrollState(int u, int v) { this.u = u; this.v = v; }
    }

    public static void scrollThumb(MatrixStack stack, int x, int y,
                                   int requestedHeight, ScrollState state) {
        bind();
        int sourceHeight = state == ScrollState.DISABLED ? 44 : 24;
        int height = Math.min(requestedHeight, sourceHeight);
        sprite(stack, x, y, state.u, state.v, 3, height);
    }
}
