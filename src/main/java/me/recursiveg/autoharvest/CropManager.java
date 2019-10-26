package me.recursiveg.autoharvest;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.passive.horse.DonkeyEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.passive.horse.LlamaEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

import java.util.Collection;

public class CropManager {
    public static final Block REED_BLOCK = Blocks.SUGAR_CANE;
    public static final Block NETHER_WART = Blocks.NETHER_WART;

    private static final ImmutableSet<Block> MOW_WEED_BLOCKS = ImmutableSet.of(
            // 6 types of sapling https://minecraft.gamepedia.com/Sapling#ID
            Blocks.OAK_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING,
            Blocks.ACACIA_SAPLING,
            Blocks.DARK_OAK_SAPLING,
            // 4 types of grass https://minecraft.gamepedia.com/Grass#ID
            Blocks.GRASS,
            Blocks.FERN,
            Blocks.TALL_GRASS,
            Blocks.LARGE_FERN,
            // Dead Bush https://minecraft.gamepedia.com/Dead_Bush#ID
            Blocks.DEAD_BUSH,
            // 2 types of small mushroom https://minecraft.gamepedia.com/Mushroom#ID
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            // 17 types of flowers https://minecraft.gamepedia.com/Flower#ID
            Blocks.DANDELION,
            Blocks.POPPY,
            Blocks.BLUE_ORCHID,
            Blocks.ALLIUM,
            Blocks.AZURE_BLUET,
            Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER,
            Blocks.LILY_OF_THE_VALLEY,
            Blocks.WITHER_ROSE,
            Blocks.SUNFLOWER,
            Blocks.LILAC,
            Blocks.ROSE_BUSH,
            Blocks.PEONY
    );

    private static final ImmutableBiMap<Block, Item> PLANT_SEED_MAP = ImmutableBiMap.<Block, Item>builder()
            .put(Blocks.WHEAT, Items.WHEAT_SEEDS)
            .put(Blocks.POTATOES, Items.POTATO)
            .put(Blocks.CARROTS, Items.CARROT)
            .put(Blocks.BEETROOTS, Items.BEETROOT_SEEDS)
            .put(Blocks.NETHER_WART, Items.NETHER_WART)
            .put(Blocks.MELON_STEM, Items.MELON_SEEDS)
            .put(Blocks.PUMPKIN_STEM, Items.PUMPKIN_SEEDS)
            .put(Blocks.SUGAR_CANE, Items.SUGAR_CANE)
            .put(Blocks.CACTUS, Items.CACTUS)
            .put(Blocks.LILY_PAD, Items.LILY_PAD)
            //.put(Blocks.BAMBOO_SAPLING, Items.BAMBOO)
            .build();

    // Check https://minecraft.gamepedia.com/Breeding for complete map
    private static final ImmutableMultimap<Item, Class<? extends AnimalEntity>> FEED_MAP = ImmutableMultimap.<Item, Class<? extends AnimalEntity>>builder()
            // Horse/donkey
            .put(Items.GOLDEN_CARROT, HorseEntity.class)
            .put(Items.GOLDEN_APPLE, HorseEntity.class)
            .put(Items.GOLDEN_CARROT, DonkeyEntity.class)
            .put(Items.GOLDEN_APPLE, DonkeyEntity.class)
            // Sheep
            .put(Items.WHEAT, SheepEntity.class)
            // Cow
            .put(Items.WHEAT, CowEntity.class)
            // Mooshroom
            .put(Items.WHEAT, MooshroomEntity.class)
            // Pig
            .put(Items.CARROT, PigEntity.class)
            .put(Items.POTATO, PigEntity.class)
            .put(Items.BEETROOT, PigEntity.class)
            // Chicken
            .put(Items.WHEAT_SEEDS, ChickenEntity.class)
            .put(Items.BEETROOT_SEEDS, ChickenEntity.class)
            .put(Items.PUMPKIN_SEEDS, ChickenEntity.class)
            .put(Items.MELON_SEEDS, ChickenEntity.class)
            // Wolf
            .put(Items.PORKCHOP, WolfEntity.class)
            .put(Items.BEEF, WolfEntity.class)
            .put(Items.CHICKEN, WolfEntity.class)
            .put(Items.RABBIT, WolfEntity.class)
            .put(Items.MUTTON, WolfEntity.class)
            .put(Items.ROTTEN_FLESH, WolfEntity.class)
            .put(Items.COOKED_PORKCHOP, WolfEntity.class)
            .put(Items.COOKED_BEEF, WolfEntity.class)
            .put(Items.COOKED_CHICKEN, WolfEntity.class)
            .put(Items.COOKED_RABBIT, WolfEntity.class)
            .put(Items.COOKED_MUTTON, WolfEntity.class)
            // Tamed Cat
            .put(Items.COD, CatEntity.class)
            .put(Items.SALMON, CatEntity.class)
            // Rabbit
            .put(Items.DANDELION, RabbitEntity.class)
            .put(Items.CARROT, RabbitEntity.class)
            .put(Items.GOLDEN_CARROT, RabbitEntity.class)
            // Llama
            .put(Items.HAY_BLOCK, LlamaEntity.class)
            // Turtle
            .put(Items.SEAGRASS, TurtleEntity.class)
            // Panda
            .put(Items.BAMBOO, PandaEntity.class)
            // Parrot
            .put(Items.WHEAT_SEEDS, ParrotEntity.class)
            .put(Items.BEETROOT_SEEDS, ParrotEntity.class)
            .put(Items.PUMPKIN_SEEDS, ParrotEntity.class)
            .put(Items.MELON_SEEDS, ParrotEntity.class)
            .build();

    // Used by MOW mode
    public static boolean isWeedBlock(World w, BlockPos pos) {
        Block b = w.getBlockState(pos).getBlock();
        return MOW_WEED_BLOCKS.contains(b);
    }

    // Used by HARVEST mode
    public static boolean isCropMature(World w, BlockPos pos, BlockState stat, Block b) {
        if (b instanceof CropsBlock) {
            return ((CropsBlock) b).isMaxAge(stat);
        } else if (b == NETHER_WART) {
            return stat.has(NetherWartBlock.AGE) && stat.get(NetherWartBlock.AGE) >= 3;
        } else if (b == REED_BLOCK) {
            Block blockDown = w.getBlockState(pos.down()).getBlock();
            Block blockDown2 = w.getBlockState(pos.down(2)).getBlock();
            return blockDown == REED_BLOCK && blockDown2 != REED_BLOCK;
        }
        return false;
    }

    /**
     * Used by FEED mode.
     * Given food item, return a list of animal types that interested in this type of food.
     */
    public static Collection<Class<? extends AnimalEntity>> getConsumerTypes(Item item) {
        return FEED_MAP.get(item);
    }

    // Used by PLANT mode
    public static EnumSeedType determineSeedType(Item seedItem) {
        if (seedItem == null) return EnumSeedType.INVALID;
        if (seedItem == Items.COCOA_BEANS) return EnumSeedType.COCOA;
        if (PLANT_SEED_MAP.containsValue(seedItem)) return EnumSeedType.NORMAL;
        return EnumSeedType.INVALID;
    }

    /**
     * Check if a new crop can be placed in this location
     * by placing on top of the supporting block
     * <p>
     * Used by PLANT mode, NORMAL type
     *
     * @param cropType
     * @param w
     * @param cropPos
     * @return
     */
    public static boolean canPlantOn(Item cropType, World w, BlockPos cropPos) {
        if (!PLANT_SEED_MAP.containsValue(cropType)) return false;
        Block cropBlock = PLANT_SEED_MAP.inverse().get(cropType);
        if (!(cropBlock instanceof IPlantable)) return false;
        BlockPos supportingPos = cropPos.down();
        BlockState supportingBlockState = w.getBlockState(supportingPos);
        Block supportingBlock = supportingBlockState.getBlock();

        if (cropType == Items.SUGAR_CANE && supportingBlock == Blocks.SUGAR_CANE) return false;
        if (cropType == Items.BAMBOO && (supportingBlock == Blocks.BAMBOO || supportingBlock == Blocks.BAMBOO_SAPLING))
            return false;
        boolean canPlaceNatively = supportingBlockState.canSustainPlant(w, supportingPos, Direction.UP, (IPlantable) cropBlock);
        return canPlaceNatively;
    }
}
