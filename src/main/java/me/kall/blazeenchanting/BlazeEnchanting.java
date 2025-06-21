package me.kall.blazeenchanting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

@Mod(BlazeEnchanting.MOD_ID)
public final class BlazeEnchanting {
    public static final String MOD_ID = "blazeenchanting";
    public static final String MOD_NAME = "BlazeEnchanting";

    public BlazeEnchanting() {
        MinecraftForge.EVENT_BUS.addListener(this::onAnvilUpdate);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.CONFIG);
    }

    public void onAnvilUpdate(@NotNull AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (right.getItem() != Items.BLAZE_POWDER) return;

        boolean isSword = left.getItem() instanceof SwordItem;
        boolean isBow = left.getItem() instanceof BowItem;
        if (!isSword && !isBow) return;

        ItemStack output = left.copy();

        Enchantment targetEnchant = isSword ? Enchantments.FIRE_ASPECT : Enchantments.FLAMING_ARROWS;
        boolean compatible = true;
        boolean hasPrevious = false;
        if (!left.getEnchantmentTags().isEmpty()) {
            for (Enchantment enchantment : EnchantmentHelper.getEnchantments(left).keySet()) {
                if (!enchantment.isCompatibleWith(targetEnchant)) {
                    if (enchantment == targetEnchant) {
                        hasPrevious = true;
                        continue;
                    }
                    compatible = false;
                    break;
                }
            }
        }
        if (!compatible) return;

        int currentLevel = output.getEnchantmentLevel(targetEnchant);

        if (currentLevel >= targetEnchant.getMaxLevel()) return;

        int newLevel = currentLevel + 1;
        if (hasPrevious) removeEnchantment(output, targetEnchant);
        output.enchant(targetEnchant, newLevel);

        event.setOutput(output);
        event.setCost(Config.LEVEL_COST.get() * newLevel);
        event.setMaterialCost(Config.MATERIAL_COST.get());
    }

    public static void removeEnchantment(@NotNull ItemStack stack, Enchantment enchantment) {
        if (stack.isEmpty()) return;

        CompoundTag tag = stack.getOrCreateTag();
        ListTag enchantmentsList = tag.getList("Enchantments", 10);

        if (enchantmentsList.isEmpty()) return;

        ResourceLocation targetId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (targetId == null) return;
        String target = targetId.toString();

        ListTag newList = new ListTag();
        boolean removed = false;

        for (int i = 0; i < enchantmentsList.size(); i++) {
            CompoundTag enchantTag = enchantmentsList.getCompound(i);
            String id = enchantTag.getString("id");

            if (!id.equals(target)) {
                newList.add(enchantTag);
            } else {
                removed = true;
            }
        }

        if (removed) {
            if (newList.isEmpty()) {
                tag.remove("Enchantments");
            } else {
                tag.put("Enchantments", newList);
            }
        }
    }

    static class Config {
        static final ForgeConfigSpec CONFIG;
        static final ForgeConfigSpec.IntValue LEVEL_COST, MATERIAL_COST;

        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            builder.push(BlazeEnchanting.MOD_NAME);
            LEVEL_COST = builder.defineInRange("LevelCost", 3, 1, Integer.MAX_VALUE);
            MATERIAL_COST = builder.defineInRange("BlazePowderCost", 2, 1, Integer.MAX_VALUE);
            builder.pop();
            CONFIG = builder.build();
        }
    }
}
