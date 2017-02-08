package org.devinprogress.autoharvest;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = "autoharvest", name = "Auto Harvest", version = "0.2")
@SideOnly(Side.CLIENT)
public class AutoHarvest {
    public enum HarvestMode {
        // SMART,  // Harvest then re-plant
        EAGER,  // Harvest only
        PLANT,  // Plant only
        SEED,   // Harvest seeds & flowers
        FEED,   // Feed animals
        OFF;    // Turn off mod
        private static HarvestMode[] vals = values();

        public AutoHarvest.HarvestMode next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    @Mod.Instance
    public static AutoHarvest instance;
    private HarvestMode mode = HarvestMode.OFF;
    private TickListener listener = null;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeyPressListener());
    }

    private void setEnabled() {
        if (listener == null) {
            listener = new TickListener(mode, 2, FMLClientHandler.instance().getClientPlayerEntity());
            MinecraftForge.EVENT_BUS.register(listener);
        }
    }

    private void setDisabled() {
        if (listener != null) {
            MinecraftForge.EVENT_BUS.unregister(listener);
            listener = null;
        }
    }

    public HarvestMode toNextMode() {
        setDisabled();
        mode = mode.next();
        if (mode != HarvestMode.OFF) {
            setEnabled();
        }
        return mode;
    }

    public void toNextMode(HarvestMode nextMode) {
        setDisabled();
        mode = nextMode;
        if (mode != HarvestMode.OFF) {
            setEnabled();
        }
    }

    public static void msg(String key, Object... obj) {
        FMLClientHandler.instance().getClient().player.sendMessage(new TextComponentString(
                I18n.format("notify.prefix")
                        + I18n.format(key, obj)
        ));
    }

    public static void moveInventoryItem(int srcIdx, int dstIdx) {
        EntityPlayerSP p = FMLClientHandler.instance().getClientPlayerEntity();
        NonNullList<ItemStack> a = p.inventory.mainInventory;
        if (a.get(srcIdx) != null) {
            p.connection.sendPacket(new CPacketClickWindow(0, srcIdx < 9 ? srcIdx + 36 : srcIdx, 0, ClickType.PICKUP, a.get(srcIdx), (short) 0));
            p.connection.sendPacket(new CPacketClickWindow(0, dstIdx < 9 ? dstIdx + 36 : dstIdx, 0, ClickType.PICKUP, a.get(dstIdx), (short) 1));
            p.connection.sendPacket(new CPacketClickWindow(0, srcIdx < 9 ? srcIdx + 36 : srcIdx, 0, ClickType.PICKUP, a.get(srcIdx), (short) 2));
//            return;
            ItemStack tmp = a.get(srcIdx);
            a.set(srcIdx, a.get(dstIdx));
            a.set(dstIdx, tmp);
        }
    }
}
