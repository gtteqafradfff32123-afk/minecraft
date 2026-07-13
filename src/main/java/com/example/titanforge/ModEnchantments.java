package com.example.titanforge;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TitanForge.MOD_ID);

    // Basic
    public static final RegistryObject<Enchantment> TITANS_WRATH = register("titans_wrath", EnchantmentType.WEAPON, 5, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> VAMPIRISM = register("vampirism", EnchantmentType.WEAPON, 5, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> CHAIN_LIGHTNING = register("chain_lightning", EnchantmentType.WEAPON, 5, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> EXECUTIONER = register("executioner", EnchantmentType.WEAPON, 5, Rarity.RARE);
    public static final RegistryObject<Enchantment> FROSTBITE = register("frostbite", EnchantmentType.WEAPON, 5, Rarity.RARE);
    public static final RegistryObject<Enchantment> TELEKINESIS = register("telekinesis", EnchantmentType.DIGGER, 1, Rarity.COMMON);
    public static final RegistryObject<Enchantment> ABYSSAL_YIELD = register("abyssal_yield", EnchantmentType.DIGGER, 3, Rarity.COMMON);
    public static final RegistryObject<Enchantment> SONIC_HASTE = register("sonic_haste", EnchantmentType.DIGGER, 3, Rarity.COMMON);

    // Magic & Elemental
    public static final RegistryObject<Enchantment> VOID_CURSE = register("void_curse", EnchantmentType.WEAPON, 3, Rarity.RARE);
    public static final RegistryObject<Enchantment> SOLAR_FLARE = register("solar_flare", EnchantmentType.BOW, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> VOID_VORTEX = register("void_vortex", EnchantmentType.BOW, 3, Rarity.VERY_RARE);

    // Dark Magic & Risk
    public static final RegistryObject<Enchantment> PLAGUE_DOCTOR = register("plague_doctor", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> BLOOD_PACT = register("blood_pact", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> SOUL_EATER = register("soul_eater", EnchantmentType.WEAPON, 5, Rarity.VERY_RARE);

    // Tactical & Defensive
    public static final EnchantmentType SHIELD = EnchantmentType.create("TITAN_SHIELD",
        item -> item == net.minecraft.item.Items.SHIELD);
    public static final RegistryObject<Enchantment> KINETIC_DEFLECTOR = register("kinetic_deflector", SHIELD, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> SHADOW_STEP = register("shadow_step", EnchantmentType.WEAPON, 1, Rarity.VERY_RARE);

    // Ranged
    public static final RegistryObject<Enchantment> RICOCHET = register("ricochet", EnchantmentType.BOW, 4, Rarity.UNCOMMON);
    public static final RegistryObject<Enchantment> CLUSTER_SHOT = register("cluster_shot", EnchantmentType.BOW, 1, Rarity.UNCOMMON);
    public static final RegistryObject<Enchantment> QUANTUM_ENTANGLEMENT = register("quantum_entanglement", EnchantmentType.WEAPON, 1, Rarity.RARE);

    // Time & Souls
    public static final EnchantmentType BOW_AND_CROSSBOW = EnchantmentType.create("BOW_AND_CROSSBOW", (item) ->
        item == net.minecraft.item.Items.BOW || item == net.minecraft.item.Items.CROSSBOW);
    public static final RegistryObject<Enchantment> CHRONO_ANCHOR = register("chrono_anchor", BOW_AND_CROSSBOW, 2, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> SOUL_REAPER = register("soul_reaper", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);

    // Chaos
    public static final RegistryObject<Enchantment> CHAOS_DEVOUR = ENCHANTMENTS.register("chaos_devour", ChaosDevourEnchantment::new);

    // Epic
    public static final EnchantmentType TITAN_ARMOR = EnchantmentType.create("TITAN_ARMOR",
        item -> item.getItem() instanceof net.minecraft.item.ArmorItem);
    public static final EnchantmentType TITAN_HELMET = EnchantmentType.create("TITAN_HELMET",
        item -> item.getItem() instanceof net.minecraft.item.ArmorItem
            && ((net.minecraft.item.ArmorItem)item.getItem()).getEquipmentSlot() == EquipmentSlotType.HEAD);

    public static final RegistryObject<Enchantment> ABSOLUTE_BLOOD = register("absolute_blood", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> HARVEST_OF_AGONY = register("harvest_of_agony", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> BLACK_SINGULARITY = register("black_singularity", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> PHASE_RUPTURE = register("phase_rupture", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> HIERARCHY_OF_ASH = register("hierarchy_of_ash", EnchantmentType.WEAPON, 3, Rarity.UNCOMMON);
    public static final RegistryObject<Enchantment> LEVIATHAN_WRATH = register("leviathan_wrath", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> CHAOS_PUPPET = register("chaos_puppet", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> NECROPOLIS_ECHO = register("necropolis_echo", EnchantmentType.WEAPON, 3, Rarity.VERY_RARE);
    public static final RegistryObject<Enchantment> ABYSS_AEGIS = register("abyss_aegis", TITAN_ARMOR, 3, Rarity.RARE);
    public static final RegistryObject<Enchantment> GAZE_OF_VOID = register("gaze_of_void", TITAN_HELMET, 3, Rarity.RARE);

    // NecroEnchants
    public static final RegistryObject<Enchantment> NECROTIC_UNDERTOW = ENCHANTMENTS.register("necrotic_undertow", NecroticUndertowEnchantment::new);
    public static final RegistryObject<Enchantment> ZEUS_VOLLEY = ENCHANTMENTS.register("zeus_volley", ZeusVolleyEnchantment::new);
    public static final RegistryObject<Enchantment> PSYCHOTIC_BREAK = ENCHANTMENTS.register("psychotic_break", PsychoticBreakEnchantment::new);
    public static final RegistryObject<Enchantment> LIMINAL_SLIP = ENCHANTMENTS.register("liminal_slip", LiminalSlipEnchantment::new);
    public static final RegistryObject<Enchantment> MEMBRANE_WEAVER = ENCHANTMENTS.register("membrane_weaver", MembraneWeaverEnchantment::new);
    public static final RegistryObject<Enchantment> TRUTH_DISSOLVER = ENCHANTMENTS.register("truth_dissolver", TruthDissolverEnchantment::new);
    public static final RegistryObject<Enchantment> UNSTABLE_EDGE =
            ENCHANTMENTS.register("unstable_edge", UnstableEdgeEnchantment::new);
    public static final RegistryObject<Enchantment> ZOMBIE_VIRUS =
            ENCHANTMENTS.register("zombie_virus", ZombieVirusEnchantment::new);

    private static RegistryObject<Enchantment> register(String name, EnchantmentType type, int maxLevel, Rarity rarity) {
        return ENCHANTMENTS.register(name, () -> new TitanEnchantment(rarity, type, maxLevel));
    }
}
