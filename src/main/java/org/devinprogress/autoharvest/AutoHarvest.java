package org.devinprogress.autoharvest;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = "autoharvest", name = "Auto Harvest", version = "0.2")
@SideOnly(Side.CLIENT)
public class AutoHarvest {
    public enum HarvestMode {
        //SMART,  // Harvest then re-plant
        EAGER,  // Harvest only
        PLANT,  // Plant only
        SEED,   // Harvest seeds & flowers
        FEED,   // Feed animals
        //ATTACK, // Attack zombies
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
        FMLCommonHandler.instance().bus().register(new KeyPressListener());
    }

    private void setEnabled() {
        if (listener == null) {
            listener = new TickListener(mode);
            FMLCommonHandler.instance().bus().register(listener);
        }
    }

    private void setDisabled() {
        if (listener != null) {
            listener.self_stop();
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

    private static void sendMessage(String msg) {
        FMLClientHandler.instance().getClient().thePlayer.addChatMessage(new ChatComponentText(msg));
    }

    public static void sendI18nMsg(String id) {
        sendMessage(i18n(id));
    }

    private static String i18n(String key) {
        return I18n.format(key);
    }
}
