package me.recursiveg.autoharvest;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("autoharvest")
public class AutoHarvest {
    // Key reg. 72 is the keycode for 'H'
    public static final KeyBinding toggleKey = new KeyBinding("key.toggle_autoharvest", 72, "key.categories.misc");
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    // Register keybinding
    static {
        ClientRegistry.registerKeyBinding(toggleKey);
    }

    // active mode
    private EnumHarvestMode mode = EnumHarvestMode.OFF;

    // ctor, Event registration
    public AutoHarvest() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Harvest mode modifications
    private TickListener listener = null;

    @SubscribeEvent
    public void onModeToggle(InputEvent.KeyInputEvent event) {
        if (toggleKey.isPressed()) {
            if (listener != null) {
                MinecraftForge.EVENT_BUS.unregister(listener);
                listener = null;
            }
            mode = mode.nextMode();
            listener = new TickListener(mode, 2, Minecraft.getInstance().player, this);
            MinecraftForge.EVENT_BUS.register(listener);
            printCurrentMode();
        }
    }

    public void modeOff() {
        if (listener != null) {
            MinecraftForge.EVENT_BUS.unregister(listener);
            listener = null;
        }
        mode = EnumHarvestMode.OFF;
        printCurrentMode();
    }

//    @SubscribeEvent
//    public void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent ev) {
//        modeOff();
//    }

    // Utils
    public void printCurrentMode() {
        boolean printLeadingSpace = true;
        StringBuilder b = new StringBuilder();
        b.append(I18n.format("notify.prefix"));
        //b.append(I18n.format("notify.current_mode"));
        for (EnumHarvestMode m : EnumHarvestMode.values()) {
            String modeName = I18n.format("mode." + m.name().toLowerCase() + ".name");
            if (m == mode) {
                b.append(ChatFormatting.GREEN);
                b.append('[');
                b.append(modeName);
                b.append(']');
                b.append(ChatFormatting.RESET);
                printLeadingSpace = false;
            } else {
                if (printLeadingSpace) b.append(' ');
                b.append(modeName);
                printLeadingSpace = true;
            }
        }
        Minecraft.getInstance().player.sendMessage(new StringTextComponent(b.toString()));
    }

    public static void msg(String key, Object... obj) {
        Minecraft.getInstance().player.sendMessage(
                new StringTextComponent(I18n.format("notify.prefix") + I18n.format(key, obj))
        );
    }

    public static void moveInventoryItem(int srcIdx, int dstIdx) {
        ClientPlayerEntity p = Minecraft.getInstance().player;
        NonNullList<ItemStack> a = p.inventory.mainInventory;
        if (a.get(srcIdx) != null) {
            p.connection.sendPacket(new CClickWindowPacket(0, srcIdx < 9 ? srcIdx + 36 : srcIdx, 0, ClickType.PICKUP, a.get(srcIdx), (short) 0));
            p.connection.sendPacket(new CClickWindowPacket(0, dstIdx < 9 ? dstIdx + 36 : dstIdx, 0, ClickType.PICKUP, a.get(dstIdx), (short) 1));
            p.connection.sendPacket(new CClickWindowPacket(0, srcIdx < 9 ? srcIdx + 36 : srcIdx, 0, ClickType.PICKUP, a.get(srcIdx), (short) 2));
            ItemStack tmp = a.get(srcIdx);
            a.set(srcIdx, a.get(dstIdx));
            a.set(dstIdx, tmp);
        }
    }
}
