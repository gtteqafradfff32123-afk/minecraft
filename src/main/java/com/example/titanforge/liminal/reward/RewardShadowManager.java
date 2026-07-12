package com.example.titanforge.liminal.reward;

import com.example.titanforge.TitanForge;
import com.example.titanforge.entities.ShadowEntity;
import com.example.titanforge.liminal.LiminalDimension;
import com.example.titanforge.liminal.LiminalManager;
import com.example.titanforge.liminal.chat.LiminalChatAI;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.UUID;

public final class RewardShadowManager {
    private static final int CHEST_SPACING = 3;

    public static void onVictory(ServerPlayerEntity player, LiminalManager.State state) {
        ServerWorld liminal = LiminalDimension.get(player.getServer());
        if (liminal == null) return;

        BlockPos center = state.center;
        spawnChest(liminal, center.add(-CHEST_SPACING, 1, 0));
        spawnChest(liminal, center.add(CHEST_SPACING, 1, 0));

        spawnDefeatedShadow(liminal, player, center);

        player.sendStatusMessage(new StringTextComponent("\u00A7a\u0422\u0432\u043E\u044F \u0442\u0435\u043D\u044C \u043F\u043E\u0432\u0435\u0440\u0436\u0435\u043D\u0430. \u0417\u0430\u0431\u0435\u0440\u0438 \u043D\u0430\u0433\u0440\u0430\u0434\u0443 \u0438 \u043F\u043E\u0433\u043E\u0432\u043E\u0440\u0438 \u0441 \u043D\u0435\u0439."), false);
        TitanForge.LOGGER.info("[liminal] victory for player={}", player.getName().getString());
    }

    private static void spawnChest(ServerWorld world, BlockPos pos) {
        world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 3);
        LockableLootTileEntity.setLootTable(world, world.rand, pos,
            new net.minecraft.util.ResourceLocation("minecraft", "chests/simple_dungeon"));
        InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY() + 1, pos.getZ(),
            new ItemStack(Items.PRISMARINE_SHARD, 3 + world.rand.nextInt(5)));
    }

    private static void spawnDefeatedShadow(ServerWorld world, ServerPlayerEntity player, BlockPos center) {
        ShadowEntity shadow = com.example.titanforge.ModEntities.SHADOW.get().create(world);
        if (shadow == null) return;

        BlockPos spawnPos = center.add(0, 1, 0);
        shadow.setLocationAndAngles(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0, 0);
        shadow.setOwner(player.getUniqueID());
        shadow.setCustomName(new StringTextComponent("\u00A77\u0422\u0435\u043D\u044C " + player.getName().getString()));
        shadow.setCustomNameVisible(true);

        DefeatedShadowTag.markDefeated(shadow, player.getUniqueID());

        if (world.addEntity(shadow)) {
            TitanForge.LOGGER.info("[liminal] defeated shadow spawned at {}", spawnPos);
            String initialPrompt = "\u0442\u044B \u043F\u043E\u0432\u0435\u0440\u0436\u0435\u043D\u0430\u044F \u0442\u0435\u043D\u044C, \u043F\u043E\u0431\u0435\u0436\u0434\u0451\u043D\u043D\u0430\u044F \u0432 \u043B\u0438\u043C\u0438\u043D\u0430\u043B\u0435, \u0442\u0432\u043E\u0440\u0438\u0442\u0435\u043B\u044C\u043D\u0430\u044F \u0438 \u0437\u0430\u0433\u0430\u0434\u043E\u0447\u043D\u0430\u044F. \u043D\u043E \u043D\u0435 \u0432\u0440\u0430\u0436\u0434\u0435\u0431\u043D\u0430\u044F. \u0442\u044B \u043C\u043E\u0436\u0435\u0448\u044C \u043E\u0442\u0432\u0435\u0447\u0430\u0442\u044C \u043D\u0430 \u0432\u043E\u043F\u0440\u043E\u0441\u044B \u0438 \u0440\u0430\u0441\u0441\u043A\u0430\u0437\u044B\u0432\u0430\u0442\u044C \u0438\u0441\u0442\u043E\u0440\u0438\u0438, \u043D\u043E \u043D\u0435 \u043F\u0440\u0435\u0434\u043B\u0430\u0433\u0430\u0442\u044C \u043F\u043E\u043C\u043E\u0449\u044C \u0432 \u0431\u043E\u044E \u0438\u043B\u0438 \u0438\u0441\u043F\u043E\u043B\u043D\u044F\u0442\u044C \u0436\u0435\u043B\u0430\u043D\u0438\u044F";
            LiminalChatAI.setSystemPrompt(player.getUniqueID(), initialPrompt);
        }
    }

    private RewardShadowManager() {}
}
