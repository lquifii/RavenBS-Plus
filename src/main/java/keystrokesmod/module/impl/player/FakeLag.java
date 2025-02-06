package keystrokesmod.module.impl.player;

import java.util.*;
import java.util.concurrent.*;

import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FakeLag extends Module {
    public SliderSetting packetDelaySlider;
    private ConcurrentSkipListMap<Long, List<Packet<?>>> packetQueue = new ConcurrentSkipListMap<>();
    private Timer timer;
    private long packetDelay;

    public FakeLag() {
        super("Fake Lag", category.player, 0);
        this.registerSetting(packetDelaySlider = new SliderSetting("Packet delay", "ms", 0.0, 0.0, 1500.0, 20.0));
    }

    @Override
    public String getInfo() {
        return packetDelay + "ms";
    }

    @Override
    public void guiUpdate() {
        if (packetDelay != packetDelaySlider.getInput()) {
            if (this.isEnabled()) {
                this.onDisable();
            }
            packetDelay = (int) packetDelaySlider.getInput();
        }
    }

    @Override
    public void onEnable() {
        if (mc.isSingleplayer()) {
            Utils.sendMessage("&cFake lag cannot be enabled in singleplayer.");
            this.disable();
            return;
        }
        if (ModuleManager.blink.isEnabled()) {
            Utils.sendMessage("&cCannot use fake lag with blink!");
            this.disable();
            return;
        }
        (timer = new Timer()).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updatePacketQueue(false);
            }
        }, 0L, 10L);
    }

    @Override
    public void onDisable() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        updatePacketQueue(true);
    }

    private void updatePacketQueue(boolean flush) {
        if (packetQueue.isEmpty()) {
            return;
        }
        if (flush) {
            for (Map.Entry<Long, List<Packet<?>>> entry : packetQueue.entrySet()) {
                for (Packet packet : entry.getValue()) {
                    PacketUtils.sendPacketNoEvent(packet);
                }
            }
            packetQueue.clear();
        }
        else {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Long, List<Packet<?>>>> it = packetQueue.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, List<Packet<?>>> entry2 = it.next();
                if (now < entry2.getKey()) {
                    break;
                }
                for (Packet packet2 : entry2.getValue()) {
                    PacketUtils.sendPacketNoEvent(packet2);
                }
                it.remove();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketSent(SendPacketEvent e) {
        if (!Utils.nullCheck() || mc.isSingleplayer() || (int) packetDelaySlider.getInput() == 0 || e.isCanceled()) {
            return;
        }
        long time = System.currentTimeMillis() + (int) packetDelaySlider.getInput();
        List<Packet<?>> packetList = packetQueue.get(time);
        if (packetList == null) {
            packetList = new ArrayList<>();
        }
        packetList.add(e.getPacket());
        packetQueue.put(time, packetList);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (mc.theWorld == null) {
            packetQueue.clear();
            this.disable();
        }
    }
}