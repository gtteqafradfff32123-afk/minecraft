package com.example.titanforge;

import com.example.titanforge.entities.PlayerCopyEntity;
import com.example.titanforge.entities.ShadowEntity;
import com.example.titanforge.entities.StunZombieEntity;
import com.example.titanforge.liminal.chat.LiminalChatAI;
import com.example.titanforge.liminal.ai.LiminalAIConfig;
import net.minecraft.entity.Entity;
import com.example.titanforge.liminal.LiminalDimension;
import com.example.titanforge.liminal.LiminalManager;
import com.example.titanforge.liminal.copy.ChunkCopyManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TitanForge.MOD_ID)
public class TitanForge {
    public static final String MOD_ID = "titanforge";
    public static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    public TitanForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEnchantments.ENCHANTMENTS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModPotions.POTIONS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModContainers.CONTAINERS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);

        modBus.addListener(this::setup);
        modBus.addListener(ModEntities::onEntityAttributeCreation);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new EventHandlerNewMagic());
        MinecraftForge.EVENT_BUS.register(new NecroEventHandler());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CapabilityHandler());
        MinecraftForge.EVENT_BUS.register(new ChunkCopyManager());
        MinecraftForge.EVENT_BUS.register(new KineticDeflectorHandler());
        MinecraftForge.EVENT_BUS.register(new com.example.titanforge.liminal.LiminalProtectionHandler());
        MinecraftForge.EVENT_BUS.register(new UnstableEdgeHandler());
        MinecraftForge.EVENT_BUS.register(new NpcEnchantHandler());
        MinecraftForge.EVENT_BUS.register(new ZombieVirusHandler());
        MinecraftForge.EVENT_BUS.register(new com.example.titanforge.liminal.LiminalArenaCleaner());

        LiminalAIConfig.register();

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                ClientConfig.SPEC,
                "titanforge-client.toml");
    }

    private void setup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();

        CapabilityManager.INSTANCE.register(MembraneCapability.MembraneData.class,
                new net.minecraftforge.common.capabilities.Capability.IStorage<MembraneCapability.MembraneData>() {
                    public net.minecraft.nbt.INBT writeNBT(net.minecraftforge.common.capabilities.Capability<MembraneCapability.MembraneData> capability, MembraneCapability.MembraneData instance, net.minecraft.util.Direction side) {
                        return instance.serialize();
                    }
                    public void readNBT(net.minecraftforge.common.capabilities.Capability<MembraneCapability.MembraneData> capability, MembraneCapability.MembraneData instance, net.minecraft.util.Direction side, net.minecraft.nbt.INBT nbt) {
                        if (nbt instanceof CompoundNBT) instance.deserialize((CompoundNBT) nbt);
                    }
                }, MembraneCapability.MembraneData::new);

        event.enqueueWork(() -> {
            net.minecraft.item.ItemStack awkward = net.minecraft.potion.PotionUtils.addPotionToItemStack(
                new net.minecraft.item.ItemStack(net.minecraft.item.Items.POTION),
                net.minecraft.potion.Potions.AWKWARD);
            net.minecraft.item.ItemStack plague = net.minecraft.potion.PotionUtils.addPotionToItemStack(
                new net.minecraft.item.ItemStack(net.minecraft.item.Items.POTION),
                ModPotions.PLAGUE_POTION.get());
            net.minecraftforge.common.brewing.BrewingRecipeRegistry.addRecipe(
                net.minecraft.item.crafting.Ingredient.fromStacks(awkward),
                net.minecraft.item.crafting.Ingredient.fromItems(net.minecraft.item.Items.ROTTEN_FLESH),
                plague);

            net.minecraft.item.ItemStack liminalPotion = net.minecraft.potion.PotionUtils.addPotionToItemStack(
                new net.minecraft.item.ItemStack(net.minecraft.item.Items.POTION),
                ModPotions.LIMINAL_POTION.get());
            net.minecraftforge.common.brewing.BrewingRecipeRegistry.addRecipe(
                net.minecraft.item.crafting.Ingredient.fromStacks(awkward),
                net.minecraft.item.crafting.Ingredient.fromItems(net.minecraft.item.Items.ENDER_PEARL),
                liminalPotion);


        });
    }

    /** Пишет зачарку в NBT напрямую (int), обходя getMaxLevel и лимит short у addEnchantment. */
    private static void applyUnlimitedEnchant(net.minecraft.item.ItemStack stack,
                                              net.minecraft.enchantment.Enchantment ench, int level) {
        String key = stack.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK ? "StoredEnchantments" : "Enchantments";
        CompoundNBT tag = stack.getOrCreateTag();
        net.minecraft.nbt.ListNBT list = tag.getList(key, 10);
        String id = String.valueOf(net.minecraft.util.registry.Registry.ENCHANTMENT.getKey(ench));
        for (int i = list.size() - 1; i >= 0; i--) {
            if (id.equals(list.getCompound(i).getString("id"))) list.remove(i);
        }
        CompoundNBT entry = new CompoundNBT();
        entry.putString("id", id);
        entry.putInt("lvl", level);
        list.add(entry);
        tag.put(key, list);
    }

    @SubscribeEvent
    public void onServerStart(FMLServerStartingEvent e) {
        LiminalDimension.syncSeedWithOverworld(e.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        // /tfenchant <зачарка> <уровень> — без ограничения максимального уровня.
        // Держишь предмет — зачарует предмет; пустая рука или обычная книга — даст зачарованную книгу.
        e.getDispatcher().register(Commands.literal("tfenchant")
            .requires(s -> s.hasPermissionLevel(2))
            .then(Commands.argument("enchantment", net.minecraft.command.arguments.EnchantmentArgument.enchantment())
                .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().asPlayer();
                        net.minecraft.enchantment.Enchantment ench =
                            net.minecraft.command.arguments.EnchantmentArgument.getEnchantment(ctx, "enchantment");
                        int level = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level");

                        net.minecraft.item.ItemStack held = player.getHeldItemMainhand();
                        net.minecraft.item.ItemStack targetStack;
                        if (held.isEmpty() || held.getItem() == net.minecraft.item.Items.BOOK) {
                            targetStack = new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENCHANTED_BOOK);
                            applyUnlimitedEnchant(targetStack, ench, level);
                            if (held.getItem() == net.minecraft.item.Items.BOOK) held.shrink(1);
                            if (player.getHeldItemMainhand().isEmpty()) {
                                player.setHeldItem(net.minecraft.util.Hand.MAIN_HAND, targetStack);
                            } else if (!player.inventory.addItemStackToInventory(targetStack)) {
                                player.dropItem(targetStack, false);
                            }
                        } else {
                            applyUnlimitedEnchant(held, ench, level);
                        }
                        ctx.getSource().sendFeedback(new StringTextComponent(
                            "§a" + ench.getDisplayName(1).getString().replaceAll(" I$", "") + " → уровень " + level), true);
                        return 1;
                    })
                )
            )
        );

        e.getDispatcher().register(Commands.literal("enchanter")
            .requires(s -> s.hasPermissionLevel(0))
            .executes(ctx -> {
                ServerPlayerEntity player = ctx.getSource().asPlayer();
                if (player != null && player.isCreative()) {
                    player.openContainer(new INamedContainerProvider() {
                        @Override
                        public ITextComponent getDisplayName() {
                            return new StringTextComponent("Enchanter");
                        }
                        @Override
                        public Container createMenu(int id, PlayerInventory inv, PlayerEntity p) {
                            return ModContainers.ENCHANTER.get().create(id, inv);
                        }
                    });
                }
                return 1;
            })
        );
    }

    @SubscribeEvent
    public void onServerChat(net.minecraftforge.event.ServerChatEvent e) {
        ServerPlayerEntity player = e.getPlayer();
        if (com.example.titanforge.liminal.reward.DefeatedShadowChatAI.onChat(player, e.getMessage())) {
            e.setCanceled(true);
            ITextComponent own = new StringTextComponent("\u00A77\u0442\u044B: \u00A7f" + e.getMessage());
            player.connection.sendPacket(new net.minecraft.network.play.server.SChatPacket(own, net.minecraft.util.text.ChatType.CHAT, player.getUniqueID()));
            return;
        }
        if (LiminalManager.isInside(player)) {
            e.setCanceled(true);
            ITextComponent own = new StringTextComponent("\u00A77\u0442\u044B: \u00A7f" + e.getMessage());
            player.connection.sendPacket(new net.minecraft.network.play.server.SChatPacket(own, net.minecraft.util.text.ChatType.CHAT, player.getUniqueID()));
            LiminalChatAI.onPlayerMessage(player, e.getMessage());
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) e.world;
        if (sw.getDimensionKey() == LiminalDimension.LIMINAL_WORLD) {
            LiminalManager.tick(sw);
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
        if (e.getEntityLiving() instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) e.getEntityLiving();
            if (LiminalManager.isInside(p)) {
                LiminalManager.requestDeathExit(p);
            } else {
                LiminalManager.onPlayerDeath(p);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        LiminalManager.flushPendingDeathExits(
            net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer());
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity)
            LiminalManager.clearLockEffects((ServerPlayerEntity) e.getPlayer());
    }

    @SubscribeEvent
    public void onDimensionChange(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getPlayer();
        if (e.getFrom() == com.example.titanforge.liminal.LiminalDimension.LIMINAL_WORLD
                && e.getTo() != com.example.titanforge.liminal.LiminalDimension.LIMINAL_WORLD) {
            TitanForge.LOGGER.info("[liminal] player changed dimension from liminal to {} — cleanup",
                e.getTo().getLocation().toString());
            LiminalManager.onExternalDimensionExit(p);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity) {
            LiminalManager.onLogout((ServerPlayerEntity) e.getPlayer());
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity)
            LiminalManager.onLogin((ServerPlayerEntity) e.getPlayer());
    }

    @SubscribeEvent
    public void onSpawn(LivingSpawnEvent.CheckSpawn e) {
        if (e.getWorld() instanceof ServerWorld
            && ((ServerWorld) e.getWorld()).getDimensionKey() == LiminalDimension.LIMINAL_WORLD) {
            Entity ent = e.getEntity();
            if (!(ent instanceof StunZombieEntity) && !(ent instanceof ShadowEntity) && !(ent instanceof PlayerCopyEntity)) {
                e.setResult(Event.Result.DENY);
            }
        }
    }
}
