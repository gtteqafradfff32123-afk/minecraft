package com.example.titanforge.liminal.reward;

import com.example.titanforge.ModEntities;
import com.example.titanforge.entities.ShadowEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class RewardShadowManager {
    private RewardShadowManager() {}

    public static void spawnAfterVictory(ServerPlayerEntity player) {
        if (!(player.world instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) player.world;
        BlockPos base = findSafeBase(world, player.getPosition().add(4, 0, 2));

        BlockPos chestA = base;
        BlockPos chestB = base.east(2);
        placeRewardChest(world, chestA, false, player);
        placeRewardChest(world, chestB, true, player);

        ShadowEntity shadow = ModEntities.SHADOW.get().create(world);
        if (shadow == null) return;

        BlockPos home = base.south(2);
        shadow.setLocationAndAngles(home.getX() + 0.5D, home.getY(),
            home.getZ() + 0.5D, 180.0F, 0.0F);
        shadow.setOwner(player.getUniqueID());
        shadow.makeDefeated(player.getUniqueID(), home);
        world.addEntity(shadow);

        player.sendMessage(new StringTextComponent(
            "\u00A78[\u0422\u0435\u043D\u044C]\u00A7f \u0422\u044B \u043F\u043E\u0431\u0435\u0434\u0438\u043B. \u042F \u044D\u0442\u043E \u043F\u0440\u0438\u0437\u043D\u0430\u044E."), player.getUniqueID());
        player.sendMessage(new StringTextComponent(
            "\u00A78[\u0422\u0435\u043D\u044C]\u00A77 \u0421\u0443\u043D\u0434\u0443\u043A\u0438 \u0442\u0432\u043E\u0438. \u0412\u0442\u043E\u0440\u043E\u0439 \u0432\u0441\u0451 \u0435\u0449\u0451 \u0443\u043C\u0435\u0435\u0442 \u043A\u0443\u0441\u0430\u0442\u044C\u0441\u044F."),
            player.getUniqueID());
    }

    private static BlockPos findSafeBase(ServerWorld world, BlockPos start) {
        BlockPos.Mutable pos = new BlockPos.Mutable(start.getX(),
            Math.min(250, start.getY() + 6), start.getZ());
        while (pos.getY() > 2 && world.isAirBlock(pos)) pos.move(0, -1, 0);
        return pos.up().toImmutable();
    }

    private static void placeRewardChest(ServerWorld world, BlockPos pos,
                                         boolean shadowChest,
                                         ServerPlayerEntity owner) {
        if (!world.isAirBlock(pos)) {
            InventoryHelper.spawnItemStack(world,
                pos.getX(), pos.getY(), pos.getZ(),
                new ItemStack(shadowChest ? Items.ENDER_CHEST : Items.CHEST));
            return;
        }

        world.setBlockState(pos,
            (shadowChest ? Blocks.TRAPPED_CHEST : Blocks.CHEST).getDefaultState(), 3);

        if (!(world.getTileEntity(pos) instanceof ChestTileEntity)) return;
        ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(pos);
        chest.getTileData().putUniqueId("TitanForgeRewardOwner", owner.getUniqueID());
        chest.getTileData().putBoolean("TitanForgeShadowReward", shadowChest);

        if (!shadowChest) {
            chest.setInventorySlotContents(10, new ItemStack(Items.DIAMOND, 3));
            chest.setInventorySlotContents(12, new ItemStack(Items.EXPERIENCE_BOTTLE, 24));
            chest.setInventorySlotContents(14, new ItemStack(Items.NETHERITE_SCRAP, 1));
            chest.setInventorySlotContents(16, new ItemStack(Items.GOLDEN_APPLE, 2));
        } else {
            ItemStack shard = new ItemStack(Items.PRISMARINE_SHARD, 1);
            shard.setDisplayName(new StringTextComponent("\u00A75\u041E\u0441\u043A\u043E\u043B\u043E\u043A \u0422\u0435\u043D\u0438"));
            shard.getOrCreateTag().putBoolean("TitanForgeShadowShard", true);
            chest.setInventorySlotContents(13, shard);
            chest.setInventorySlotContents(11, new ItemStack(Items.ENDER_PEARL, 8));
            chest.setInventorySlotContents(15, new ItemStack(Items.TOTEM_OF_UNDYING, 1));
        }
        chest.markDirty();
    }

    public static boolean handleChat(ServerPlayerEntity player, String raw) {
        ShadowEntity shadow = findOwnedShadow(player, 64.0D);
        if (shadow == null) return false;

        String msg = normalize(raw);
        boolean addressed = msg.startsWith("\u0442\u0435\u043D\u044C") || msg.startsWith("\u044D\u0439 \u0442\u0435\u043D\u044C");
        boolean follow = containsAny(msg,
            "\u0438\u0434\u0438 \u0437\u0430 \u043C\u043D\u043E\u0439", "\u0441\u043B\u0435\u0434\u0443\u0439 \u0437\u0430 \u043C\u043D\u043E\u0439", "\u043F\u043E\u0439\u0434\u0435\u043C", "\u043F\u043E\u0439\u0434\u0451\u043C",
            "\u0438\u0434\u0438 \u0441\u043E \u043C\u043D\u043E\u0439", "\u043C\u043E\u0436\u0435\u0448\u044C \u0438\u0434\u0442\u0438 \u0441\u043E \u043C\u043D\u043E\u0439", "\u043A\u043E \u043C\u043D\u0435", "\u0438\u0434\u0438 \u0441\u044E\u0434\u0430");
        boolean stay = containsAny(msg,
            "\u043D\u0435 \u0445\u043E\u0434\u0438 \u0437\u0430 \u043C\u043D\u043E\u0439", "\u0441\u0442\u043E\u0439 \u0437\u0434\u0435\u0441\u044C", "\u043E\u0441\u0442\u0430\u043D\u044C\u0441\u044F", "\u043E\u0441\u0442\u0430\u0432\u0430\u0439\u0441\u044F",
            "\u0436\u0434\u0438 \u0443 \u0441\u0443\u043D\u0434\u0443\u043A\u043E\u0432", "\u0432\u0435\u0440\u043D\u0438\u0441\u044C \u043A \u0441\u0443\u043D\u0434\u0443\u043A\u0430\u043C", "\u0438\u0434\u0438 \u043A \u0441\u0443\u043D\u0434\u0443\u043A\u0430\u043C");

        if (follow) {
            shadow.setDefeatedMode(ShadowEntity.DefeatedMode.FOLLOW);
            DefeatedShadowChatAI.reply(player, raw,
                "\u0418\u0433\u0440\u043E\u043A \u043F\u0440\u0438\u043A\u0430\u0437\u0430\u043B \u0438\u0434\u0442\u0438 \u0437\u0430 \u043D\u0438\u043C. \u0422\u044B \u043F\u043E\u0434\u0447\u0438\u043D\u0438\u043B\u0441\u044F \u0438 \u043F\u0440\u0438\u0437\u043D\u0430\u043B \u043A\u043E\u043C\u0430\u043D\u0434\u0443.");
            return true;
        }

        if (stay) {
            shadow.setDefeatedMode(ShadowEntity.DefeatedMode.RETURN_HOME);
            DefeatedShadowChatAI.reply(player, raw,
                "\u0418\u0433\u0440\u043E\u043A \u043F\u0440\u0438\u043A\u0430\u0437\u0430\u043B \u043D\u0435 \u0441\u043B\u0435\u0434\u043E\u0432\u0430\u0442\u044C \u0437\u0430 \u043D\u0438\u043C \u0438 \u0432\u0435\u0440\u043D\u0443\u0442\u044C\u0441\u044F \u043A \u0441\u0443\u043D\u0434\u0443\u043A\u0430\u043C.");
            return true;
        }

        if (addressed) {
            DefeatedShadowChatAI.reply(player, raw,
                "\u0422\u044B \u0441\u0442\u043E\u0438\u0448\u044C \u0440\u044F\u0434\u043E\u043C \u0441 \u043D\u0430\u0433\u0440\u0430\u0434\u043D\u044B\u043C\u0438 \u0441\u0443\u043D\u0434\u0443\u043A\u0430\u043C\u0438 \u043F\u043E\u0441\u043B\u0435 \u0441\u0432\u043E\u0435\u0433\u043E \u043F\u043E\u0440\u0430\u0436\u0435\u043D\u0438\u044F.");
            return true;
        }

        return false;
    }

    private static ShadowEntity findOwnedShadow(ServerPlayerEntity player, double range) {
        List<ShadowEntity> shadows = player.world.getEntitiesWithinAABB(
            ShadowEntity.class,
            new AxisAlignedBB(player.getPosition()).grow(range),
            e -> e.isDefeated()
                && player.getUniqueID().equals(e.getDefeatedOwner()));
        return shadows.isEmpty() ? null : shadows.get(0);
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT)
            .replace('\u0451', '\u0435')
            .replaceAll("[^\\u0430-\\u044F\\u0435a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            String normalized = variant.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435');
            if (text.contains(normalized)) return true;
        }
        return false;
    }
}
