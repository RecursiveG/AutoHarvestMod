package org.devinprogress.autoharvest;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import java.util.*;

/**
 * Auto Harvest Mod
 * This Mod can help you harvest crops automatically.
 * It can also help you clear the ground.
 * Works for both SSP & SMP
 * This Mod is published under GPLv3
 * Use it *AT YOUR OWN RISK*
 *                   RecursiveG
 *                   2014 Sept. 29th
 *
 *
 *
 * Auto Harvest Mod
 * Copyright (C) 2014  RecursiveG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@Mod(modid="autoharvest", name="Auto Harvest Mod", version="1.0")
public class AutoHarvest {
    private boolean enabled=false;
    private boolean harvestTick=true;
    private KeyBinding toggleKey=new KeyBinding("Toggle Enabled/Disabled", Keyboard.KEY_H,"Auto Harvest Mod");
    private static int harvestRange=1;
    private Minecraft mc=null;

    private static final Set<Integer> grassBlockIds =new HashSet<Integer>(Arrays.asList(new Integer[]{
            6,31,32,37,38,39,40,175
    }));
    private static final Set<Item> plantableItems=new HashSet<Item>(){{
        add(Items.wheat_seeds);
        add(Items.carrot);
        add(Items.potato);
        add(Items.melon_seeds);
        add(Items.pumpkin_seeds);
    }};
    private static final Map<Class<?>,Item> harvestMap=new HashMap<Class<?>,Item>(){{
        put(BlockCrops.class,Items.wheat_seeds);
        put(BlockCarrot.class,Items.carrot);
        put(BlockPotato.class,Items.potato);
        put(BlockNetherWart.class,Items.nether_wart);
    }};
    static final Map<Class<?>,Integer> cropMatureData=new HashMap<Class<?>, Integer>(){{
        put(BlockCrops.class,7);
        put(BlockCarrot.class,7);
        put(BlockPotato.class,7);
        put(BlockNetherWart.class,3);
    }};


    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void load(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    private void sendPlayerPrivateMsg(String str){
        FMLClientHandler.instance().getClient().thePlayer.addChatMessage(new ChatComponentText(str));
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void onToggle(InputEvent.KeyInputEvent e){
        if(toggleKey.isPressed()){
            if(!enabled){
                enabled=true;
                mc=FMLClientHandler.instance().getClient();
                sendPlayerPrivateMsg("[Auto Harvest] Enabled");
            }else{
                enabled=false;
                sendPlayerPrivateMsg("[Auto Harvest] Disabled");
            }
        }
    }

    @SubscribeEvent
    public void onInGameTick(TickEvent.PlayerTickEvent e){//one block a tick
        if(enabled && e.side==Side.CLIENT && e.player!=null){
            if(e.player.inventory.getCurrentItem()==null){
                doClearGrass(e.player);
            }else{
                if(harvestTick)
                    doHarvest(e.player);
                else
                    doPlant(e.player);
                harvestTick=!harvestTick;
            }
        }
    }

    private void doClearGrass(EntityPlayer p){
        World w=p.worldObj;
        int X=(int)Math.floor(p.posX);
        int Y=(int)Math.floor(p.posY-1.45);//the "leg block"
        int Z=(int)Math.floor(p.posZ);
        for(int deltaY=-1;deltaY<=1;++deltaY)
            for(int deltaX=-2;deltaX<=2;++deltaX)
                for(int deltaZ=-2;deltaZ<=2;++deltaZ)
                    if(grassBlockIds.contains(Block.getIdFromBlock(w.getBlock(X+deltaX,Y+deltaY,Z+deltaZ)))){
                        mc.playerController.onPlayerDamageBlock(X+deltaX,Y+deltaY,Z+deltaZ,1);
                        return;
                    }
    }

    private void doHarvest(EntityPlayer p){
        World w=p.worldObj;
        int X=(int)Math.floor(p.posX);
        int Y=(int)Math.floor(p.posY-1.45);//the "leg block"
        int Z=(int)Math.floor(p.posZ);
        for(int deltaX=-harvestRange;deltaX<=harvestRange;++deltaX)
            for(int deltaZ=-harvestRange;deltaZ<=harvestRange;++deltaZ){
                if(canHarvest(w,p,X+deltaX,Y,Z+deltaZ)) {
                    mc.playerController.onPlayerDamageBlock(X+deltaX,Y,Z+deltaZ,1);
                    return;
                }
            }
    }

    private void doPlant(EntityPlayer p){
        World w=p.worldObj;
        int X=(int)Math.floor(p.posX);
        int Y=(int)Math.floor(p.posY-2.45);//Block player stand on;
        int Z=(int)Math.floor(p.posZ);
        for(int deltaX=-harvestRange;deltaX<=harvestRange;++deltaX)
            for(int deltaZ=-harvestRange;deltaZ<=harvestRange;++deltaZ){
                if(canPlantOn(w,p,X+deltaX,Y,Z+deltaZ)) {
                    ItemStack seed=mc.thePlayer.inventory.getCurrentItem();
                    mc.playerController.onPlayerRightClick(p,w,seed,X+deltaX,Y,Z+deltaZ,1,
                            Vec3.createVectorHelper(X+deltaX+0.5,Y+1,Z+deltaZ+0.5));
                    return;
                }
            }
    }

    private boolean canHarvest(World w,EntityPlayer p,int X,int Y,int Z){
        Class<?> c=w.getBlock(X, Y, Z).getClass();
        return harvestMap.containsKey(c) && cropMatureData.get(c)==w.getBlockMetadata(X,Y,Z) &&
               harvestMap.get(c) == p.inventory.getCurrentItem().getItem();
    }

    private boolean canPlantOn(World w,EntityPlayer p,int X,int Y,int Z){
        Item i=p.inventory.getCurrentItem().getItem();
        return w.getBlock(X,Y+1,Z)instanceof BlockAir &&
               (w.getBlock(X,Y,Z) instanceof BlockFarmland && plantableItems.contains(i) ||
                w.getBlock(X,Y,Z) instanceof BlockSoulSand && i==Items.nether_wart);
    }
}
