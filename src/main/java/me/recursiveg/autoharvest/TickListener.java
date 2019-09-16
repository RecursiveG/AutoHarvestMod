package me.recursiveg.autoharvest;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;

public class TickListener {
    private final EnumHarvestMode mode;
    private final int range;
    private final ClientPlayerEntity p;
    private final AutoHarvest mod;

    public TickListener(EnumHarvestMode mode, int range, ClientPlayerEntity player, AutoHarvest mod) {
        this.mode = mode;
        this.range = range;
        this.p = player;
        this.mod = mod;
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent e) {
        try {
            if (e.side.isServer() || e.player != p) return;
            switch (mode) {
                case MOW:
                    mowTick();
                    break;
                case HARVEST:
                    harvestTick();
                    break;
                case PLANT:
                    plantTick();
                    break;
                case FEED:
                    feedTick();
                    break;
                case SHEAR:
                    shearTick();
                    break;
            }
        } catch (Exception ex) {
            mod.modeOff();
            AutoHarvest.msg("notify.tick_error");
            AutoHarvest.msg("notify.switch_to.off");
            ex.printStackTrace();
        }
    }

    /* clear all grass on land */
    private void mowTick() {
        World w = p.getEntityWorld();
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY);//the "leg block"
        int Z = (int) Math.floor(p.posZ);
        for (int deltaY = 2; deltaY >= -1; --deltaY) {
            for (int deltaX = -range; deltaX <= range; ++deltaX) {
                for (int deltaZ = -range; deltaZ <= range; ++deltaZ) {
                    BlockPos pos = new BlockPos(X + deltaX, Y + deltaY, Z + deltaZ);

                    if (CropManager.isWeedBlock(w, pos)) {
                        Minecraft.getInstance().playerController.onPlayerDamageBlock(pos, Direction.UP);
                        return;
                    }

                }
            }
        }
    }

    /* harvest all mature crops */
    private void harvestTick() {
        World w = p.getEntityWorld();
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY + 0.2D);//the "leg block", in case in soul sand
        int Z = (int) Math.floor(p.posZ);
        for (int deltaX = -range; deltaX <= range; ++deltaX) {
            for (int deltaZ = -range; deltaZ <= range; ++deltaZ) {
                for (int deltaY = -1; deltaY <= 1; ++deltaY) {
                    BlockPos pos = new BlockPos(X + deltaX, Y + deltaY, Z + deltaZ);

                    BlockState state = w.getBlockState(pos);
                    Block b = state.getBlock();
                    if (CropManager.isCropMature(w, pos, state, b)) {
                        Minecraft.getInstance().playerController.onPlayerDamageBlock(pos, Direction.UP);
                        return;
                    }
                    
                }
            }
        }
    }

    private void feedTick() {
        ItemStack handItem = tryFillItemInHand();
        if (handItem == null) return;
        Collection<Class<? extends AnimalEntity>> animalList = CropManager.getConsumerTypes(handItem.getItem());
        AxisAlignedBB box = new AxisAlignedBB(p.posX - range, p.posY - range, p.posZ - range,
                p.posX + range, p.posY + range, p.posZ + range);
        for (Class<? extends AnimalEntity> type : animalList) {
            for (AnimalEntity e : p.getEntityWorld().getEntitiesWithinAABB(type, box)) {
                if (e.getGrowingAge() >= 0 && !e.isInLove()) {
                    lastUsedItem = handItem.copy();
                    ActionResultType result = Minecraft.getInstance().playerController.interactWithEntity(p, e, Hand.MAIN_HAND);
                }
            }
        }
    }

    private void plantTick() {
        ItemStack handItem = tryFillItemInHand();
        if (handItem == null) return;
        switch (CropManager.determineSeedType(handItem.getItem())) {
            case COCOA:
                plantCocoaTick(handItem);
                break;
            case NORMAL:
                plantNormalTick(handItem);
                break;
        }
    }

    private void plantNormalTick(ItemStack handItem) {
        World w = p.getEntityWorld();
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY + 0.2D);//the "leg block" , in case in soul sand
        int Z = (int) Math.floor(p.posZ);
        for (int deltaY = -1; deltaY <= 1; ++deltaY) {
            for (int deltaX = -range; deltaX <= range; ++deltaX) {
                for (int deltaZ = -range; deltaZ <= range; ++deltaZ) {
                    BlockPos pos = new BlockPos(X + deltaX, Y + deltaY, Z + deltaZ);

                    if (w.getBlockState(pos).getBlock() != Blocks.AIR) continue;
                    if (CropManager.canPlantOn(handItem.getItem(), w, pos)) {
                        BlockPos downPos = pos.down();
                        int afterPlantItemCount = handItem.getCount() - 1;
                        lastUsedItem = handItem.copy();
                        BlockRayTraceResult clickPoint = new BlockRayTraceResult(new Vec3d(X + deltaX + 0.5, Y + deltaY, Z + deltaZ + 0.5), Direction.UP, downPos, false);

                        Minecraft.getInstance().playerController.func_217292_a(
                                p,
                                Minecraft.getInstance().world,
                                Hand.MAIN_HAND, clickPoint);
                        setHandItemCount(afterPlantItemCount);
                        return;
                    }

                }
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
                    BlockState jungleBlock = w.getBlockState(pos);
                    if (jungleBlock.getBlock() == Blocks.JUNGLE_LOG) {
                        BlockPos tmpPos;

                        Direction tmpFace = Direction.EAST;
                        tmpPos = pos.add(tmpFace.getDirectionVec());
                        if (w.getBlockState(tmpPos).getBlock() == Blocks.AIR) {
                            lastUsedItem = handItem.copy();
                            int afterUseItemCount = handItem.getCount() - 1;
                            BlockRayTraceResult clickPoint = new BlockRayTraceResult(new Vec3d(X + deltaX + 1, Y + deltaY + 0.5, Z + deltaZ + 0.5), tmpFace, pos, false);
                            Minecraft.getInstance().playerController.func_217292_a(
                                    p,
                                    Minecraft.getInstance().world,
                                    Hand.MAIN_HAND, clickPoint);
                            setHandItemCount(afterUseItemCount);
                            return;
                        }

                        tmpFace = Direction.WEST;
                        tmpPos = pos.add(tmpFace.getDirectionVec());
                        if (w.getBlockState(tmpPos).getBlock() == Blocks.AIR) {
                            lastUsedItem = handItem.copy();
                            int afterUseItemCount = handItem.getCount() - 1;
                            BlockRayTraceResult clickPoint = new BlockRayTraceResult(new Vec3d(X + deltaX, Y + deltaY + 0.5, Z + deltaZ + 0.5), tmpFace, pos, false);
                            Minecraft.getInstance().playerController.func_217292_a(
                                    p,
                                    Minecraft.getInstance().world,
                                    Hand.MAIN_HAND, clickPoint);
                            setHandItemCount(afterUseItemCount);
                            return;
                        }

                        tmpFace = Direction.SOUTH;
                        tmpPos = pos.add(tmpFace.getDirectionVec());
                        if (w.getBlockState(tmpPos).getBlock() == Blocks.AIR) {
                            lastUsedItem = handItem.copy();
                            int afterUseItemCount = handItem.getCount() - 1;

                            BlockRayTraceResult clickPoint = new BlockRayTraceResult(new Vec3d(X + deltaX + 0.5, Y + deltaY + 0.5, Z + deltaZ + 1), tmpFace, pos, false);
                            Minecraft.getInstance().playerController.func_217292_a(
                                    p,
                                    Minecraft.getInstance().world,
                                    Hand.MAIN_HAND, clickPoint);
                            setHandItemCount(afterUseItemCount);
                            return;
                        }

                        tmpFace = Direction.NORTH;
                        tmpPos = pos.add(tmpFace.getDirectionVec());
                        if (w.getBlockState(tmpPos).getBlock() == Blocks.AIR) {
                            lastUsedItem = handItem.copy();
                            int afterUseItemCount = handItem.getCount() - 1;

                            BlockRayTraceResult clickPoint = new BlockRayTraceResult(new Vec3d(X + deltaX + 0.5, Y + deltaY + 0.5, Z + deltaZ), tmpFace, pos, false);
                            Minecraft.getInstance().playerController.func_217292_a(
                                    p,
                                    Minecraft.getInstance().world,
                                    Hand.MAIN_HAND, clickPoint);
                            setHandItemCount(afterUseItemCount);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void shearTick() {
        AxisAlignedBB box = new AxisAlignedBB(p.posX - range, p.posY - range, p.posZ - range,
                p.posX + range, p.posY + range, p.posZ + range);
        ItemStack handItem = p.getHeldItem(Hand.MAIN_HAND);
        if (handItem.getItem() == Items.SHEARS) {
            for (SheepEntity e : p.getEntityWorld().getEntitiesWithinAABB(SheepEntity.class, box)) {
                if (!e.isChild() && !e.getSheared()) {
                    Minecraft.getInstance().playerController.interactWithEntity(p, e, Hand.MAIN_HAND);
                    return;
                }
            }
        }
    }

    private void setHandItemCount(int count) {
        if (count <= 0) {
            p.setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY);
        } else {
            p.getHeldItem(Hand.MAIN_HAND).setCount(count);
        }
    }

    private ItemStack lastUsedItem = null;

    private ItemStack tryFillItemInHand() {
        ItemStack itemStack = p.getHeldItem(Hand.MAIN_HAND);
        if (itemStack.isEmpty()) {
            int supplmentIdx = -1;
            ItemStack stack = null;
            if (lastUsedItem != null && !lastUsedItem.isEmpty()) {
                NonNullList<ItemStack> inv = p.inventory.mainInventory;
                for (int idx = 0; idx < 36; ++idx) {
                    ItemStack s = inv.get(idx);
                    if (s.getItem() == lastUsedItem.getItem() &&
                            s.getDamage() == lastUsedItem.getDamage() &&
                            !s.hasTag()) {
                        supplmentIdx = idx;
                        stack = s;
                        break;
                    }
                }
            } else {
                return null;
            }
            if (supplmentIdx < 0) {
                AutoHarvest.msg("notify.lack_of_seed");
                mod.modeOff();
                lastUsedItem = null;
                return null;
            }
            AutoHarvest.moveInventoryItem(supplmentIdx, p.inventory.currentItem);
            return stack;
        } else {
            return itemStack;
        }
    }

    private boolean canReachBlock(ClientPlayerEntity playerEntity, BlockPos blockpos) {
        double d0 = playerEntity.posX - ((double) blockpos.getX() + 0.5D);
        double d1 = playerEntity.posY - ((double) blockpos.getY() + 0.5D) + 1.5D;
        double d2 = playerEntity.posZ - ((double) blockpos.getZ() + 0.5D);
        double d3 = d0 * d0 + d1 * d1 + d2 * d2;
        return d3 <= 36D;
    }
}
