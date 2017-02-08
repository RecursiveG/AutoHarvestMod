package me.recursiveg.autoharvest;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collection;

public class TickListener {
    private final AutoHarvest.HarvestMode mode;
    private final int range;
    private final EntityPlayerSP p;

    public TickListener(AutoHarvest.HarvestMode mode, int range, EntityPlayerSP player) {
        this.mode = mode;
        this.range = range;
        this.p = player;
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent e) {
        try {
            if (e.side != Side.CLIENT || e.player != p) return;
            switch (mode) {
                // case SMART: smartTick();break;
                case SEED:
                    seedTick();
                    break;
                case EAGER:
                    harvestTick();
                    break;
                case PLANT:
                    plantTick();
                    break;
                case FEED:
                    feedTick();
                    break;
            }
        } catch (Exception ex) {
            AutoHarvest.msg("notify.tick_error");
            AutoHarvest.msg("notify.switch_to.off");
            ex.printStackTrace();
            AutoHarvest.instance.toNextMode(AutoHarvest.HarvestMode.OFF);
        }
    }

    /* clear all grass on land */
    private void seedTick() {
        World w = p.getEntityWorld();
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY);//the "leg block"
        int Z = (int) Math.floor(p.posZ);
        for (int deltaY = 2; deltaY >= -1; --deltaY)
            for (int deltaX = -range; deltaX <= range; ++deltaX)
                for (int deltaZ = -range; deltaZ <= range; ++deltaZ) {
                    BlockPos pos = new BlockPos(X + deltaX, Y + deltaY, Z + deltaZ);
                    if (CropManager.isWeedBlock(w, pos)) {
                        Minecraft.getMinecraft().playerController.onPlayerDamageBlock(pos, EnumFacing.UP);
                        return;
                    }
                }
    }

    /* harvest all mature crops */
    private void harvestTick() {
        World w = p.getEntityWorld();
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY + 0.2D);//the "leg block", in case in soul sand
        int Z = (int) Math.floor(p.posZ);
        for (int deltaX = -range; deltaX <= range; ++deltaX)
            for (int deltaZ = -range; deltaZ <= range; ++deltaZ) {
                for (int deltaY = -1; deltaY <= 1; ++deltaY) {
                    BlockPos pos = new BlockPos(X + deltaX, Y + deltaY, Z + deltaZ);
                    IBlockState state = w.getBlockState(pos);
                    Block b = state.getBlock();
                    if (CropManager.isCropMature(w, pos, state, b)) {
                        Minecraft.getMinecraft().playerController.onPlayerDamageBlock(pos, EnumFacing.UP);
                        return;
                    }
                }
            }
    }

    private void minusOneInHand() {
        ItemStack st = p.getHeldItem(EnumHand.MAIN_HAND);
        if (st != null) {
            if (st.getCount() <= 1) {
                p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            } else {
                st.setCount(st.getCount() - 1);
            }
        }
    }

    private ItemStack lastUsedItem = null;

    private ItemStack tryFillItemInHand() {
        ItemStack itemStack = p.getHeldItem(EnumHand.MAIN_HAND);
        if (itemStack == null) {
            int supplmentIdx = -1;
            ItemStack stack = null;
            if (lastUsedItem != null) {
                ItemStack[] inv = (ItemStack[]) p.inventory.mainInventory.toArray();
                for (int idx = 0; idx < 36; ++idx) {
                    if (inv[idx] != null && inv[idx].getItem() == lastUsedItem.getItem() &&
                            inv[idx].getItemDamage() == lastUsedItem.getItemDamage() &&
                            inv[idx].hasTagCompound() == false) {
                        supplmentIdx = idx;
                        stack = inv[idx];
                        break;
                    }
                }
            } else {
                return null;
            }
            if (supplmentIdx < 0) {
                AutoHarvest.msg("notify.lack_of_seed");
                AutoHarvest.msg("notify.switch_to.off");
                AutoHarvest.instance.toNextMode(AutoHarvest.HarvestMode.OFF);
                lastUsedItem = null;
                return null;
            }
            AutoHarvest.moveInventoryItem(supplmentIdx, p.inventory.currentItem);
            return stack;
        } else {
            return itemStack;
        }
    }

    private void plantTick() {
        ItemStack handItem = tryFillItemInHand();
        if (handItem == null) return;
        if (!CropManager.isSeed(handItem)) {
            if (CropManager.isCocoa(handItem)) {
                plantCocoaTick(handItem);
            }
            return;
        }

        World w = p.getEntityWorld();
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY + 0.2D);//the "leg block" , in case in soul sand
        int Z = (int) Math.floor(p.posZ);

        for (int deltaX = -range; deltaX <= range; ++deltaX)
            for (int deltaZ = -range; deltaZ <= range; ++deltaZ) {
                BlockPos pos = new BlockPos(X + deltaX, Y, Z + deltaZ);
                if (w.getBlockState(pos).getBlock() != Blocks.AIR) continue;
                if (CropManager.canPlantOn(handItem.getItem(), w, pos)) {
                    BlockPos downPos = pos.down();
                    FMLClientHandler.instance().getClient().playerController.processRightClickBlock(
                            p,
                            FMLClientHandler.instance().getWorldClient(),
                            downPos, EnumFacing.UP,
                            new Vec3d(X + deltaX + 0.5, Y, Z + deltaZ + 0.5),
                            EnumHand.MAIN_HAND);
                    lastUsedItem = handItem;
                    minusOneInHand();
                    return;
                }
            }
    }

    private void plantCocoaTick(ItemStack handItem) {
        World w = p.getEntityWorld();
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY + 0.2D);//the "leg block" , in case in soul sand
        int Z = (int) Math.floor(p.posZ);

        for (int deltaX = -range; deltaX <= range; ++deltaX) {
            for (int deltaZ = -range; deltaZ <= range; ++deltaZ) {
                for (int deltaY = 0; deltaY <= 7; ++deltaY) {
                    BlockPos pos = new BlockPos(X + deltaX, Y + deltaY, Z + deltaZ);
                    if (!canReachBlock(p, pos)) continue;
                    IBlockState jungleBlock = w.getBlockState(pos);
                    if (CropManager.isJungleLog(jungleBlock)) {
                        BlockPos tmpPos;

                        EnumFacing tmpFace = EnumFacing.EAST;
                        tmpPos = pos.add(tmpFace.getDirectionVec());
                        if (w.getBlockState(tmpPos).getBlock() == Blocks.AIR) {
                            FMLClientHandler.instance().getClient().playerController.processRightClickBlock(
                                    p,
                                    FMLClientHandler.instance().getWorldClient(),
                                    pos, tmpFace,
                                    new Vec3d(X + deltaX + 1, Y + deltaY + 0.5, Z + deltaZ + 0.5),
                                    EnumHand.MAIN_HAND);
                            lastUsedItem = handItem;
                            minusOneInHand();
                            return;
                        }

                        tmpFace = EnumFacing.WEST;
                        tmpPos = pos.add(tmpFace.getDirectionVec());
                        if (w.getBlockState(tmpPos).getBlock() == Blocks.AIR) {
                            FMLClientHandler.instance().getClient().playerController.processRightClickBlock(
                                    p,
                                    FMLClientHandler.instance().getWorldClient(),
                                    pos, tmpFace,
                                    new Vec3d(X + deltaX, Y + deltaY + 0.5, Z + deltaZ + 0.5),
                                    EnumHand.MAIN_HAND);
                            lastUsedItem = handItem;
                            minusOneInHand();
                            return;
                        }

                        tmpFace = EnumFacing.SOUTH;
                        tmpPos = pos.add(tmpFace.getDirectionVec());
                        if (w.getBlockState(tmpPos).getBlock() == Blocks.AIR) {
                            FMLClientHandler.instance().getClient().playerController.processRightClickBlock(
                                    p,
                                    FMLClientHandler.instance().getWorldClient(),
                                    pos, tmpFace,
                                    new Vec3d(X + deltaX + 0.5, Y + deltaY + 0.5, Z + deltaZ + 1),
                                    EnumHand.MAIN_HAND);
                            lastUsedItem = handItem;
                            minusOneInHand();
                            return;
                        }

                        tmpFace = EnumFacing.NORTH;
                        tmpPos = pos.add(tmpFace.getDirectionVec());
                        if (w.getBlockState(tmpPos).getBlock() == Blocks.AIR) {
                            FMLClientHandler.instance().getClient().playerController.processRightClickBlock(
                                    p,
                                    FMLClientHandler.instance().getWorldClient(),
                                    pos, tmpFace,
                                    new Vec3d(X + deltaX + 0.5, Y + deltaY + 0.5, Z + deltaZ),
                                    EnumHand.MAIN_HAND);
                            lastUsedItem = handItem;
                            minusOneInHand();
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean canReachBlock(EntityPlayerSP playerEntity, BlockPos blockpos) {
        double d0 = playerEntity.posX - ((double) blockpos.getX() + 0.5D);
        double d1 = playerEntity.posY - ((double) blockpos.getY() + 0.5D) + 1.5D;
        double d2 = playerEntity.posZ - ((double) blockpos.getZ() + 0.5D);
        double d3 = d0 * d0 + d1 * d1 + d2 * d2;
        return d3 <= 36D;
    }

    private void feedTick() {
        ItemStack handItem = tryFillItemInHand();
        if (handItem == null) return;
        Collection<Class<? extends EntityAnimal>> animalList = CropManager.FEED_MAP.get(handItem.getItem());
        AxisAlignedBB box = new AxisAlignedBB(p.posX - range, p.posY - range, p.posZ - range,
                p.posX + range, p.posY + range, p.posZ + range);
        for (Class<? extends EntityAnimal> type : animalList) {
            for (EntityAnimal e : p.getEntityWorld().getEntitiesWithinAABB(type, box)) {
                if (e.getGrowingAge() >= 0 && !e.isInLove()) {
                    EnumActionResult result = FMLClientHandler.instance().getClient().playerController
                            .interactWithEntity(p, e, EnumHand.MAIN_HAND);
                    if (result == EnumActionResult.SUCCESS) {
                        lastUsedItem = handItem;
                        minusOneInHand();
                        return;
                    }
                }
            }
        }
        if (handItem.getItem() == Items.SHEARS) {
            for (EntitySheep e : p.getEntityWorld().getEntitiesWithinAABB(EntitySheep.class, box)) {
                if (!e.isChild() && !e.getSheared()) {
                    FMLClientHandler.instance().getClient().playerController
                            .interactWithEntity(p, e, EnumHand.MAIN_HAND);
                    lastUsedItem = handItem;
                    return;
                }
            }
        }
    }
}
