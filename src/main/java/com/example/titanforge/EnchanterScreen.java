package com.example.titanforge;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.RegistryObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnchanterScreen extends ContainerScreen<EnchanterContainer> {
    private static final int UI_WIDTH = 300;
    private static final int UI_HEIGHT = 230;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 36;
    private static final int LIST_W = 170;
    private static final int ENTRY_H = 12;
    private static final int MAX_VISIBLE = 15;
    private static final int DESC_X = 183;
    private static final int DESC_W = 110;
    private static final int DESC_Y = 50;
    private static final int DESC_H = 170;

    private static final int COLOR_COMMON = 0xFFFFFF;
    private static final int COLOR_UNCOMMON = 0x55FF55;
    private static final int COLOR_RARE = 0x5555FF;
    private static final int COLOR_VERY_RARE = 0xAA55FF;
    private static final int COLOR_EPIC = 0xFFAA00;
    private static final int COLOR_LEGENDARY = 0xFF5555;
    private static final int COLOR_MYTHIC = 0xFF55FF;

    private static final String ICON_MELEE = "\u2694";
    private static final String ICON_RANGED = "\uD83C\uDFF9";
    private static final String ICON_TOOL = "\u26CF";
    private static final String ICON_ARMOR = "\uD83D\uDEE1";
    private static final String ICON_HELMET = "\uD83D\uDC51";

    private static class EnchantInfo {
        final RegistryObject<Enchantment> enchant;
        final String equipIcon;
        final int color;
        final int sortOrder;

        EnchantInfo(RegistryObject<Enchantment> enchant, String equipIcon, int color, int sortOrder) {
            this.enchant = enchant;
            this.equipIcon = equipIcon;
            this.color = color;
            this.sortOrder = sortOrder;
        }
    }

    private static final List<EnchantInfo> ALL_ENCHANTS = Arrays.asList(
        new EnchantInfo(ModEnchantments.TELEKINESIS, ICON_TOOL, COLOR_COMMON, 1),
        new EnchantInfo(ModEnchantments.SONIC_HASTE, ICON_TOOL, COLOR_COMMON, 2),
        new EnchantInfo(ModEnchantments.ABYSSAL_YIELD, ICON_TOOL, COLOR_COMMON, 3),
        new EnchantInfo(ModEnchantments.CLUSTER_SHOT, ICON_RANGED, COLOR_UNCOMMON, 4),
        new EnchantInfo(ModEnchantments.RICOCHET, ICON_RANGED, COLOR_UNCOMMON, 6),
        new EnchantInfo(ModEnchantments.HIERARCHY_OF_ASH, ICON_MELEE, COLOR_UNCOMMON, 7),
        new EnchantInfo(ModEnchantments.VOID_CURSE, ICON_MELEE, COLOR_RARE, 9),
        new EnchantInfo(ModEnchantments.QUANTUM_ENTANGLEMENT, ICON_MELEE, COLOR_RARE, 10),
        new EnchantInfo(ModEnchantments.ABYSS_AEGIS, ICON_ARMOR, COLOR_RARE, 11),
        new EnchantInfo(ModEnchantments.GAZE_OF_VOID, ICON_HELMET, COLOR_RARE, 12),
        new EnchantInfo(ModEnchantments.VAMPIRISM, ICON_MELEE, COLOR_VERY_RARE, 13),
        new EnchantInfo(ModEnchantments.CHAIN_LIGHTNING, ICON_MELEE, COLOR_VERY_RARE, 14),
        new EnchantInfo(ModEnchantments.SHADOW_STEP, ICON_MELEE, COLOR_VERY_RARE, 15),
        new EnchantInfo(ModEnchantments.PLAGUE_DOCTOR, ICON_MELEE, COLOR_VERY_RARE, 16),
        new EnchantInfo(ModEnchantments.HARVEST_OF_AGONY, ICON_MELEE, COLOR_VERY_RARE, 17),
        new EnchantInfo(ModEnchantments.TITANS_WRATH, ICON_MELEE, COLOR_EPIC, 18),
        new EnchantInfo(ModEnchantments.EXECUTIONER, ICON_MELEE, COLOR_EPIC, 19),
        new EnchantInfo(ModEnchantments.FROSTBITE, ICON_MELEE, COLOR_EPIC, 20),
        new EnchantInfo(ModEnchantments.SOUL_EATER, ICON_MELEE, COLOR_EPIC, 21),
        new EnchantInfo(ModEnchantments.VOID_VORTEX, ICON_RANGED, COLOR_EPIC, 22),
        new EnchantInfo(ModEnchantments.CHRONO_ANCHOR, ICON_RANGED, COLOR_EPIC, 23),
        new EnchantInfo(ModEnchantments.KINETIC_DEFLECTOR, ICON_ARMOR, COLOR_EPIC, 24),
        new EnchantInfo(ModEnchantments.PHASE_RUPTURE, ICON_MELEE, COLOR_EPIC, 25),
        new EnchantInfo(ModEnchantments.CHAOS_PUPPET, ICON_MELEE, COLOR_EPIC, 26),
        new EnchantInfo(ModEnchantments.ABSOLUTE_BLOOD, ICON_MELEE, COLOR_EPIC, 27),
        new EnchantInfo(ModEnchantments.LEVIATHAN_WRATH, ICON_MELEE, COLOR_EPIC, 28),
        new EnchantInfo(ModEnchantments.BLOOD_PACT, ICON_MELEE, COLOR_LEGENDARY, 29),
        new EnchantInfo(ModEnchantments.BLACK_SINGULARITY, ICON_MELEE, COLOR_LEGENDARY, 30),
        new EnchantInfo(ModEnchantments.NECROPOLIS_ECHO, ICON_MELEE, COLOR_LEGENDARY, 31),
        new EnchantInfo(ModEnchantments.SOUL_REAPER, ICON_MELEE, COLOR_LEGENDARY, 32),
        new EnchantInfo(ModEnchantments.CHAOS_DEVOUR, ICON_MELEE, COLOR_LEGENDARY, 33),
        new EnchantInfo(ModEnchantments.SOLAR_FLARE, ICON_RANGED, COLOR_MYTHIC, 34),
        new EnchantInfo(ModEnchantments.NECROTIC_UNDERTOW, ICON_MELEE, COLOR_MYTHIC, 35),
        new EnchantInfo(ModEnchantments.ZEUS_VOLLEY, ICON_RANGED, COLOR_MYTHIC, 36),
        new EnchantInfo(ModEnchantments.PSYCHOTIC_BREAK, ICON_MELEE, COLOR_MYTHIC, 37),
        new EnchantInfo(ModEnchantments.LIMINAL_SLIP, ICON_MELEE, COLOR_MYTHIC, 38),
        new EnchantInfo(ModEnchantments.MEMBRANE_WEAVER, ICON_ARMOR, COLOR_MYTHIC, 39),
        new EnchantInfo(ModEnchantments.TRUTH_DISSOLVER, ICON_MELEE, COLOR_MYTHIC, 40)
    );

    private static final String[] TABS = { "All", "Melee", "Ranged", "Tool", "Armor" };
    private int activeTab = 0;

    private TextFieldWidget searchField;
    private String searchQuery = "";
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int descScrollOffset = 0;
    private List<EnchantInfo> filteredList = new ArrayList<>();
    private Enchantment hoveredEnchant = null;
    private List<String> wrappedDescLines = new ArrayList<>();
    private List<String> wrappedHelpLines = new ArrayList<>();

    public EnchanterScreen(EnchanterContainer container, PlayerInventory inv, net.minecraft.util.text.ITextComponent title) {
        super(container, inv, title);
        this.xSize = UI_WIDTH;
        this.ySize = UI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        rebuildFilter();

        this.searchField = new TextFieldWidget(this.font, this.guiLeft + LIST_X, this.guiTop + 23, LIST_W, 11, StringTextComponent.EMPTY);
        this.searchField.setMaxStringLength(32);
        this.searchField.setResponder(s -> {
            searchQuery = s;
            rebuildFilter();
        });
        this.children.add(this.searchField);
    }

    private void rebuildFilter() {
        filteredList.clear();
        String query = searchQuery.toLowerCase().trim();
        for (EnchantInfo info : ALL_ENCHANTS) {
            boolean tabMatch = true;
            if (activeTab != 0) {
                String tab = TABS[activeTab];
                String icon = info.equipIcon;
                if (tab.equals("Melee") && !icon.equals(ICON_MELEE)) tabMatch = false;
                else if (tab.equals("Ranged") && !icon.equals(ICON_RANGED)) tabMatch = false;
                else if (tab.equals("Tool") && !icon.equals(ICON_TOOL)) tabMatch = false;
                else if (tab.equals("Armor") && !(icon.equals(ICON_ARMOR) || icon.equals(ICON_HELMET))) tabMatch = false;
            }
            if (!tabMatch) continue;

            if (!query.isEmpty()) {
                String name = I18n.format("enchantment.titanforge." + info.enchant.get().getRegistryName().getPath()).toLowerCase();
                if (!name.contains(query)) continue;
            }

            filteredList.add(info);
        }
        if (selectedIndex >= filteredList.size()) selectedIndex = filteredList.size() - 1;
        if (selectedIndex < 0) selectedIndex = 0;
        scrollOffset = 0;
        descScrollOffset = 0;
        rebuildDescLines();
    }

    private void rebuildDescLines() {
        wrappedDescLines.clear();
        wrappedHelpLines.clear();
        descScrollOffset = 0;

        Enchantment ench = getSelectedEnchantment();
        if (ench == null) return;
        ResourceLocation rl = ench.getRegistryName();
        if (rl == null) return;
        String path = rl.getPath();

        String desc = I18n.format("enchantment." + rl.getNamespace() + "." + path + ".desc");
        if (!desc.contains("enchantment.")) {
            wrappedDescLines.addAll(wrapText(desc, DESC_W));
        }

        String help = I18n.format("enchantment." + rl.getNamespace() + "." + path + ".help");
        if (!help.contains("enchantment.")) {
            wrappedHelpLines.addAll(wrapText(help, DESC_W));
        }
    }

    private Enchantment getSelectedEnchantment() {
        EnchantInfo info = selectedIndex < filteredList.size() ? filteredList.get(selectedIndex) : null;
        return info != null ? info.enchant.get() : null;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.searchField.render(matrixStack, mouseX, mouseY, partialTicks);

        int left = this.guiLeft;
        int top = this.guiTop;

        if (this.searchField.getText().isEmpty() && !this.searchField.isFocused()) {
            this.font.drawString(matrixStack, "\uD83D\uDD0D " + I18n.format("gui.titanforge.search"), left + LIST_X + 2, top + 24, 0x666666);
        }

        int tabX = left + LIST_X;
        for (int i = 0; i < TABS.length; i++) {
            int tw = font.getStringWidth(TABS[i]) + 10;
            int tabColor = i == activeTab ? 0xFF555555 : 0xFF333333;
            fill(matrixStack, tabX, top + 6, tabX + tw, top + 22, tabColor);
            font.drawString(matrixStack, TABS[i], tabX + 5, top + 10, i == activeTab ? 0xFFAA00 : 0xA0A0A0);
            tabX += tw + 2;
        }

        // Description panel
        Enchantment displayEnch = hoveredEnchant != null ? hoveredEnchant : getSelectedEnchantment();
        if (displayEnch != null) {
            ResourceLocation rl = displayEnch.getRegistryName();
            if (rl != null) {
                String path = rl.getPath();
                String name = I18n.format("enchantment." + rl.getNamespace() + "." + path);
                String desc = I18n.format("enchantment." + rl.getNamespace() + "." + path + ".desc");

                if (!desc.contains("enchantment.")) {
                    int descTop = top + LIST_Y;
                    this.font.drawString(matrixStack, name, left + DESC_X, descTop + 2, 0xFFAA00);

                    int lineY = descTop + 14;
                    int visibleHeight = DESC_H - 16;
                    for (int i = descScrollOffset; i < wrappedDescLines.size(); i++) {
                        if (lineY + 10 > descTop + DESC_H) break;
                        this.font.drawString(matrixStack, wrappedDescLines.get(i), left + DESC_X, lineY, 0xA0A0A0);
                        lineY += 10;
                    }

                    // Help text
                    if (!wrappedHelpLines.isEmpty()) {
                        lineY += 4;
                        for (String helpLine : wrappedHelpLines) {
                            if (lineY + 10 > descTop + DESC_H) break;
                            this.font.drawString(matrixStack, helpLine, left + DESC_X, lineY, 0xFFAA00);
                            lineY += 10;
                        }
                    }

                    // Description scroll arrow
                    int totalLines = wrappedDescLines.size() + wrappedHelpLines.size();
                    int maxLines = (DESC_H - 20) / 10;
                    if (totalLines > maxLines) {
                        int arrowY = descTop + DESC_H - 10;
                        if (descScrollOffset + maxLines < totalLines) {
                            this.font.drawString(matrixStack, "\u25BC", left + DESC_X + DESC_W - 6, arrowY, 0x888888);
                        }
                    }
                }
            }
        }

        // List scroll arrows
        int listBottom = top + LIST_Y + MAX_VISIBLE * ENTRY_H + 2;
        if (scrollOffset > 0) {
            this.font.drawString(matrixStack, "\u25B2", left + LIST_X + LIST_W - 10, top + LIST_Y - 1, 0xFFFFFF);
        }
        if (scrollOffset + MAX_VISIBLE < filteredList.size()) {
            this.font.drawString(matrixStack, "\u25BC", left + LIST_X + LIST_W - 10, listBottom + 1, 0xFFFFFF);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        int left = this.guiLeft;
        int top = this.guiTop;

        fill(matrixStack, left, top, left + UI_WIDTH, top + UI_HEIGHT, 0xFF2D2D2D);
        fill(matrixStack, left + 1, top + 1, left + UI_WIDTH - 1, top + UI_HEIGHT - 1, 0xFF1A1A1A);

        fill(matrixStack, left + LIST_X - 2, top + LIST_Y - 2,
                left + LIST_X + LIST_W + 2, top + LIST_Y + MAX_VISIBLE * ENTRY_H + 2, 0xCC000000);

        fill(matrixStack, left + DESC_X - 2, top + LIST_Y - 2,
                left + DESC_X + DESC_W + 10, top + LIST_Y + DESC_H + 2, 0x40000000);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
        hoveredEnchant = null;
        for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < filteredList.size(); i++) {
            int idx = i + scrollOffset;
            int entryY = LIST_Y + i * ENTRY_H;
            EnchantInfo info = filteredList.get(idx);
            Enchantment ench = info.enchant.get();

            int relX = x - guiLeft;
            int relY = y - guiTop;
            boolean hovered = relX >= LIST_X && relX < LIST_X + LIST_W && relY >= entryY && relY < entryY + ENTRY_H;
            if (idx == selectedIndex) {
                fill(matrixStack, LIST_X, entryY, LIST_X + LIST_W, entryY + ENTRY_H, 0x8040FF40);
            } else if (hovered) {
                fill(matrixStack, LIST_X, entryY, LIST_X + LIST_W, entryY + ENTRY_H, 0x40FFFFFF);
            }
            if (hovered) {
                hoveredEnchant = ench;
            }
            ResourceLocation rl = ench.getRegistryName();
            String name = I18n.format(rl != null ? "enchantment." + rl.getNamespace() + "." + rl.getPath() : new TranslationTextComponent(ench.getName()).getKey());
            this.font.drawString(matrixStack, info.equipIcon + " " + name, LIST_X + 3, entryY + 2, info.color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = this.guiLeft;
        int top = this.guiTop;

        int tabX = left + LIST_X;
        for (int i = 0; i < TABS.length; i++) {
            int tw = font.getStringWidth(TABS[i]) + 10;
            if (mouseX >= tabX && mouseX < tabX + tw && mouseY >= top + 6 && mouseY < top + 22) {
                if (activeTab != i) {
                    activeTab = i;
                    rebuildFilter();
                }
                return true;
            }
            tabX += tw + 2;
        }

        int listLeft = left + LIST_X;
        int listTop = top + LIST_Y;
        for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < filteredList.size(); i++) {
            int idx = i + scrollOffset;
            int entryY = listTop + i * ENTRY_H;
            if (mouseX >= listLeft && mouseX < listLeft + LIST_W && mouseY >= entryY && mouseY < entryY + ENTRY_H) {
                if (selectedIndex != idx) {
                    selectedIndex = idx;
                    rebuildDescLines();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int left = this.guiLeft;
        int top = this.guiTop;

        // List scroll
        if (mouseX >= left + LIST_X && mouseX <= left + LIST_X + LIST_W
                && mouseY >= top + LIST_Y && mouseY <= top + LIST_Y + MAX_VISIBLE * ENTRY_H + 2) {
            int maxScroll = Math.max(0, filteredList.size() - MAX_VISIBLE);
            if (delta < 0) {
                scrollOffset = Math.min(scrollOffset + 1, maxScroll);
            } else {
                scrollOffset = Math.max(0, scrollOffset - 1);
            }
            return true;
        }

        // Description scroll
        if (mouseX >= left + DESC_X && mouseX <= left + DESC_X + DESC_W + 10
                && mouseY >= top + LIST_Y && mouseY <= top + LIST_Y + DESC_H) {
            int totalLines = wrappedDescLines.size() + wrappedHelpLines.size();
            int maxLines = (DESC_H - 20) / 10;
            int maxDescScroll = Math.max(0, totalLines - maxLines);
            if (delta < 0) {
                descScrollOffset = Math.min(descScrollOffset + 1, maxDescScroll);
            } else {
                descScrollOffset = Math.max(0, descScrollOffset - 1);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        String currentColor = "\u00A77";
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (this.font.getStringWidth(test) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                currentColor = extractCurrentColor(line.toString());
                line = new StringBuilder(currentColor + word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private String extractCurrentColor(String text) {
        String[] parts = text.split("\u00A7");
        if (parts.length > 1) {
            return "\u00A7" + parts[parts.length - 1].charAt(0);
        }
        return "\u00A77";
    }
}
