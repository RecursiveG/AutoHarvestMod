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
import ibxm.Player;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.lwjgl.input.Keyboard;

/**
 * Created by recursiveg on 14-9-28.
 */

@Mod(modid="autoharvest", name="Auto Harvest", version="0.1-dev")
public class AutoHarvest {
    private boolean enabled=false;
    private boolean harvestTick=true;
    private KeyBinding toggleKey=new KeyBinding("Toggle Status", Keyboard.KEY_H,"Auto Harvest Mod");
    private static int[] delta={-1,0,1};

    @Mod.EventHandler
    public void load(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onToggle(InputEvent.KeyInputEvent e){
        if(toggleKey.isPressed()){
            //off->harvest->plant
            /*if(!enabled){
                enabled=true;
                harvestTick=true;
                FMLClientHandler.instance().getClient().thePlayer.addChatMessage(new ChatComponentText("[Auto Harvest] Current Mode: Harvest"));
            }else if(enabled&&harvestTick){
                harvestTick=false;
                FMLClientHandler.instance().getClient().thePlayer.addChatMessage(new ChatComponentText("[Auto Harvest] Current Mode: Plant"));
            }else{
                enabled=false;
                FMLClientHandler.instance().getClient().thePlayer.addChatMessage(new ChatComponentText("[Auto Harvest] Current Mode: OFF"));
            }*/
            if(!enabled){
                enabled=true;
                FMLClientHandler.instance().getClient().thePlayer.addChatMessage(new ChatComponentText("[Auto Harvest] Current Mode: ON"));
            }else{
                enabled=false;
                FMLClientHandler.instance().getClient().thePlayer.addChatMessage(new ChatComponentText("[Auto Harvest] Current Mode: OFF"));
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent e){
        if(enabled && e.side==Side.CLIENT && e.player!=null){
            harvestTick=!harvestTick;
            if(harvestTick)
                doHarvest(e.player);
            else
                doPlant(e.player);
        }
    }

    private void doHarvest(EntityPlayer p){
        World w=p.worldObj;
        int X=(int)Math.floor(p.posX);
        int Y=(int)Math.floor(p.posY-1.61);
        int Z=(int)Math.floor(p.posZ);
        for(int deltaX:delta)
            for(int deltaZ:delta){
                if(canHarvest(w,X+deltaX,Y,Z+deltaZ)) {
                    FMLClientHandler.instance().getClient().playerController.onPlayerDamageBlock(X + deltaX, Y, Z + deltaZ, 1);
                    return;
                }
            }
    }

    private boolean canHarvest(World w,int X,int Y,int Z){
        return (w.getBlock(X,Y,Z)instanceof BlockCrops&&w.getBlockMetadata(X,Y,Z)==7);
    }

    private void doPlant(EntityPlayer p){
        World w=p.worldObj;
        int X=(int)Math.floor(p.posX);
        int Y=(int)Math.floor(p.posY-1.61);
        int Z=(int)Math.floor(p.posZ);

        Y=Y-1;
        for(int deltaX:delta)
            for(int deltaZ:delta){
                if(canPlantOn(w, X + deltaX, Y, Z + deltaZ)) {
                    ItemStack seed=selectSeed(p);
                    if(seed!=null)
                        FMLClientHandler.instance().getClient().playerController.onPlayerRightClick(p,w,seed,X + deltaX, Y, Z + deltaZ,1,
                                Vec3.createVectorHelper(X + deltaX+0.5, Y+1, Z + deltaZ+0.5));
                    else{
                        //System.out.println("seed==null");
                    }
                    return;
                }
            }
    }

    private boolean canPlantOn(World w,int X,int Y,int Z){
        return (w.getBlock(X,Y,Z) instanceof BlockFarmland)&&(w.getBlock(X,Y+1,Z)instanceof BlockAir);
    }

    private ItemStack selectSeed(EntityPlayer p){
        if(p==null||p.inventory==null||p.inventory.getCurrentItem()==null)return null;
        if(p.inventory.getCurrentItem().getItem() instanceof ItemSeeds)
            return p.inventory.getCurrentItem();
        else
            return null;
    }
}
