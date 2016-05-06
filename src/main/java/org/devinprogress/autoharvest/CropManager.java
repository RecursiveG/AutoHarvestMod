package org.devinprogress.autoharvest;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CropManager {
    public static final Block REED_BLOCK = Block.getBlockFromName("reeds");
    public static final Block FARMLAND = Blocks.farmland;
    public static final Block NETHER_WART = Blocks.nether_wart;


    public static final Set<Block> WEED_BLOCKS = new HashSet<Block>() {{
        add(Block.getBlockFromName("sapling"));
        add(Block.getBlockFromName("tallgrass"));
        add(Block.getBlockFromName("deadbush"));
        add(Block.getBlockFromName("yellow_flower"));
        add(Block.getBlockFromName("red_flower"));
        add(Block.getBlockFromName("brown_mushroom"));
        add(Block.getBlockFromName("red_mushroom"));
        add(Block.getBlockFromName("double_plant"));
    }};

    public static final Map<Block, Item> CROP_BLOCKS = new HashMap<Block, Item>() {{
        put(Block.getBlockFromName("wheat"), Item.getByNameOrId("wheat_seeds"));
        put(Blocks.potatoes, Items.potato);
        put(Blocks.carrots, Items.carrot);
    }};

    public static final Set<Item> SEED_ITEMS = new HashSet<Item>() {{
        add(Items.wheat_seeds);
        add(Items.potato);
        add(Items.carrot);
        add(Items.nether_wart);
        add(Items.reeds);
    }};

    public static boolean isWeedBlock(World w, BlockPos pos) {
        Block b = w.getBlockState(pos).getBlock();
        return WEED_BLOCKS.contains(b);
    }

    public static boolean isCropMature(World w, BlockPos pos, IBlockState stat, Block b) {
        if (CROP_BLOCKS.containsKey(b)) {
            return b.getMetaFromState(stat) >= 7;
        } else if (b == NETHER_WART) {
            return b.getMetaFromState(stat) >= 3;
        } else if (b == REED_BLOCK) {
            if (w.getBlockState(pos.down()).getBlock() != REED_BLOCK
                    && w.getBlockState(pos.up()).getBlock() == REED_BLOCK) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static Item getSeedItem(Block b) {
        if (CROP_BLOCKS.containsKey(b)) {
            return CROP_BLOCKS.get(b);
        } else if (b == NETHER_WART) {
            return Items.nether_wart;
        } else {
            return null;
        }
    }

    public static boolean isSeed(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return false;
        return SEED_ITEMS.contains(stack.getItem());
    }

    public static boolean canPlantOn(Item m, World w, BlockPos p, Block b) {
        if (m == Items.nether_wart) {
            return b == Blocks.soul_sand;
        } else if (m == Items.reeds) {
            return b == Blocks.sand || b == Blocks.grass || b == Blocks.dirt;
        } else {
            return b == FARMLAND;
        }
    }
}
