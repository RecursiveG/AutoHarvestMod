package org.devinprogress.autoharvest;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TickListener {
    private int tickCount = 0;
    private static final int tickRate = 2;
    private static final int seedRange = 3;
    private static final int harvestRange = 2;
    private final AutoHarvest.HarvestMode mode;

    public TickListener(AutoHarvest.HarvestMode mode) {
        this.mode = mode;
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent e) {
        if (e.side == Side.CLIENT && e.player != null &&
                e.player == FMLClientHandler.instance().getClientPlayerEntity()) {
            if ((++tickCount) == tickRate) {
                tickCount = 0;
                switch (mode) {
                    case SEED: {
                        seedTick(e.player);
                        break;
                    }
                    case EAGER: {
                        harvestTick(e.player);
                        break;
                    }
                    case PLANT: {
                        plantTick(e.player);
                        break;
                    }
                    case FEED: {
                        feedTick(e.player);
                        break;
                    }
//                    case ATTACK: {
//                        attackTick(e.player);
//                        break;
//                    }
                }
            }
        }
    }

    private void seedTick(EntityPlayer p) {
        World w = p.worldObj;
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY);//the "leg block"
        int Z = (int) Math.floor(p.posZ);
        for (int deltaY = 1; deltaY >= -1; --deltaY)
            for (int deltaX = -seedRange; deltaX <= seedRange; ++deltaX)
                for (int deltaZ = -seedRange; deltaZ <= seedRange; ++deltaZ) {
                    BlockPos pos = new BlockPos(X + deltaX, Y + deltaY, Z + deltaZ);
                    if (CropManager.isWeedBlock(w, pos)) {
                        Minecraft.getMinecraft().playerController.onPlayerDamageBlock(pos, EnumFacing.UP);
                        return;
                    }
                }
    }

    private void harvestTick(EntityPlayer p) {
        World w = p.worldObj;
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY + 0.2D);//the "leg block", in case in soul sand
        int Z = (int) Math.floor(p.posZ);
        for (int deltaX = -harvestRange; deltaX <= harvestRange; ++deltaX)
            for (int deltaZ = -harvestRange; deltaZ <= harvestRange; ++deltaZ) {
                BlockPos pos = new BlockPos(X + deltaX, Y, Z + deltaZ);
                IBlockState state = w.getBlockState(pos);
                Block b = state.getBlock();
                if (CropManager.isCropMature(w, pos, state, b)) {
                    if (b == Blocks.reeds) pos = pos.up();
                    Minecraft.getMinecraft().playerController.onPlayerDamageBlock(pos, EnumFacing.UP);
                    return;
                }
            }
    }

    private static final int REFILL_THRESHOLD = 2;

    private boolean tryFillItemInHand(EntityPlayer p) {
        InventoryPlayer inv = p.inventory;
        ItemStack itemStack = inv.getCurrentItem();
        if (itemStack == null || itemStack.stackSize > REFILL_THRESHOLD) return false;
        Class<? extends Item> targetItemClass = itemStack.getItem().getClass();

        for (int i = 0; i < 9; i++) {
            if (inv.getStackInSlot(i) == null) continue;
            if (inv.getStackInSlot(i).getItem().getClass().equals(targetItemClass) && inv.getStackInSlot(i).stackSize > REFILL_THRESHOLD) {
                while (i < inv.currentItem) inv.changeCurrentItem(1);
                while (i > inv.currentItem) inv.changeCurrentItem(-1);
                return true;
            }
        }
        AutoHarvest.sendI18nMsg("notify.lack_of_seed");
        AutoHarvest.sendI18nMsg("notify.switch_to.off");
        AutoHarvest.instance.toNextMode(AutoHarvest.HarvestMode.OFF);

        return false;
    }

    private void plantTick(EntityPlayer p) {
        ItemStack handItem = p.getHeldItem();
        if (!CropManager.isSeed(handItem)) {
            return;
        }

        World w = p.worldObj;
        int X = (int) Math.floor(p.posX);
        int Y = (int) Math.floor(p.posY + 0.2D);//the "leg block" , in case in soul sand
        int Z = (int) Math.floor(p.posZ);

        for (int deltaX = -harvestRange; deltaX <= harvestRange; ++deltaX)
            for (int deltaZ = -harvestRange; deltaZ <= harvestRange; ++deltaZ) {
                BlockPos pos = new BlockPos(X + deltaX, Y, Z + deltaZ);
                BlockPos downPos = pos.down();
                Block downBlock = w.getBlockState(downPos).getBlock();
                if (w.getBlockState(pos).getBlock() == Blocks.air
                        && downBlock != Blocks.air
                        && CropManager.canPlantOn(handItem.getItem(), w, downPos, downBlock)) {
                    if (handItem.stackSize <= REFILL_THRESHOLD && tryFillItemInHand(p))
                        handItem = p.getHeldItem();
                    FMLClientHandler.instance().getClient().playerController.onPlayerRightClick(
                            FMLClientHandler.instance().getClientPlayerEntity(),
                            FMLClientHandler.instance().getWorldClient(),
                            handItem, downPos, EnumFacing.UP,
                            new Vec3(X + deltaX + 0.5, Y, Z + deltaZ + 0.5));
                }
            }
    }

    private boolean tryFeedAnimal(Class<? extends EntityAnimal> type, AxisAlignedBB box, EntityPlayer p) {
        for (EntityAnimal e : (List<EntityAnimal>) (p.getEntityWorld().getEntitiesWithinAABB(type, box))) {
            if (e.getGrowingAge() == 0 && !e.isInLove()) {
                FMLClientHandler.instance().getClient().playerController.interactWithEntitySendPacket(p, e);
                return true;
            }
        }
        return false;
    }

    private void feedTick(EntityPlayer p) {
        ItemStack handItem = p.getHeldItem();
        if (handItem == null) return;
        if (handItem.getItem().equals(Items.carrot)) { //pig & rabbit
            if (handItem.stackSize <= REFILL_THRESHOLD) tryFillItemInHand(p);
            AxisAlignedBB box = AxisAlignedBB.fromBounds(p.posX - 2, p.posY - 1, p.posZ - 2, p.posX + 2, p.posY + 3, p.posZ + 2);
            if (tryFeedAnimal(EntityPig.class, box, p)) return;
            if (tryFeedAnimal(EntityRabbit.class, box, p)) return;
        } else if (handItem.getItem().equals(Items.wheat)) { //cow & sheep & mooshrom
            if (handItem.stackSize <= REFILL_THRESHOLD) tryFillItemInHand(p);
            AxisAlignedBB box = AxisAlignedBB.fromBounds(p.posX - 2, p.posY - 1, p.posZ - 2, p.posX + 2, p.posY + 3, p.posZ + 2);
            if (tryFeedAnimal(EntityCow.class, box, p)) return;
            if (tryFeedAnimal(EntityMooshroom.class, box, p)) return;
            if (tryFeedAnimal(EntitySheep.class, box, p)) return;
        } else if (handItem.getItem().equals(Items.wheat_seeds)) { //chicken
            if (handItem.stackSize <= REFILL_THRESHOLD) tryFillItemInHand(p);
            AxisAlignedBB box = AxisAlignedBB.fromBounds(p.posX - 2, p.posY - 1, p.posZ - 2, p.posX + 2, p.posY + 3, p.posZ + 2);
            if (tryFeedAnimal(EntityChicken.class, box, p)) return;
        } else if (handItem.getItem().equals(Items.shears)) { // wool
            AxisAlignedBB box = AxisAlignedBB.fromBounds(p.posX - 2, p.posY - 1, p.posZ - 2, p.posX + 2, p.posY + 3, p.posZ + 2);
            for (EntitySheep e : (List<EntitySheep>) (p.getEntityWorld().getEntitiesWithinAABB(EntitySheep.class, box))) {
                if (!e.isChild() && !e.getSheared()) {
                    FMLClientHandler.instance().getClient().playerController.interactWithEntitySendPacket(p, e);
                    return;
                }
            }
        }
    }

    private long gameTick = 0;
    private static final int ATTACK_GAP = 5;
    private Map<Entity, Long> attackTime = new HashMap<Entity, Long>();

    private void attackTick(EntityPlayer p) {
        gameTick += tickRate;
        AxisAlignedBB box = AxisAlignedBB.fromBounds(p.posX - 2, p.posY - 2, p.posZ - 2, p.posX + 2, p.posY + 3, p.posZ + 2);
        List<Entity> aroundMobs = p.getEntityWorld().getEntitiesWithinAABB(EntityZombie.class, box);
        for (Entity e : aroundMobs) {
            Long t = attackTime.get(e);
            if (t == null || t < gameTick) {
                FMLClientHandler.instance().getClient().playerController.attackEntity(p, e);
                attackTime.put(e, gameTick);
                return;
            }
        }
    }

    public void self_stop() {
        // CLEANUP
        FMLCommonHandler.instance().bus().unregister(this);
    }
}
