package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.minigames.BedWars;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BedAura extends Module {
    public SliderSetting mode;
    private SliderSetting breakSpeed;
    private SliderSetting fov;
    public SliderSetting range;
    private SliderSetting rate;
    public ButtonSetting allowAura;
    private ButtonSetting breakNearBlock;
    private ButtonSetting cancelKnockback;
    public ButtonSetting disableBHop;
    private ButtonSetting disableBreakEffects;
    public ButtonSetting groundSpoof;
    public ButtonSetting ignoreSlow;
    private ButtonSetting onlyWhileVisible;
    private ButtonSetting renderOutline;
    private ButtonSetting sendAnimations;
    private ButtonSetting silentSwing;
    private String[] modes = new String[] { "Legit", "Instant", "Swap" };

    private BlockPos[] bedPos;
    public float breakProgress;
    private int lastSlot = -1;
    public boolean rotate;
    public BlockPos currentBlock;
    private long lastCheck = 0;
    public boolean stopAutoblock;
    private int outlineColor = new Color(226, 65, 65).getRGB();
    private BlockPos nearestBlock;
    private Map<BlockPos, Float> breakProgressMap = new HashMap<>();
    public double lastProgress;
    public float vanillaProgress;
    private int defaultOutlineColor = new Color(226, 65, 65).getRGB();
    private boolean aiming;
    private int noAutoBlockTicks;
    private BlockPos previousBlockBroken;
    private BlockPos rotateLastBlock;

    public BedAura() {
        super("BedAura", category.player, 0);
        this.registerSetting(mode = new SliderSetting("Break mode", 0, modes));
        this.registerSetting(breakSpeed = new SliderSetting("Break speed", "x", 1, 0.8, 2, 0.01));
        this.registerSetting(fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
        this.registerSetting(range = new SliderSetting("Range", 4.5, 1.0, 8.0, 0.5));
        this.registerSetting(rate = new SliderSetting("Rate", " second", 0.2, 0.05, 3.0, 0.05));
        this.registerSetting(allowAura = new ButtonSetting("Allow aura", true));
        this.registerSetting(breakNearBlock = new ButtonSetting("Break near block", false));
        this.registerSetting(cancelKnockback = new ButtonSetting("Cancel knockback", false));
        this.registerSetting(disableBHop = new ButtonSetting("Disable bhop", false));
        this.registerSetting(disableBreakEffects = new ButtonSetting("Disable break effects", false));
        this.registerSetting(groundSpoof = new ButtonSetting("Ground spoof", false));
        this.registerSetting(ignoreSlow = new ButtonSetting("Ignore slow", false));
        this.registerSetting(onlyWhileVisible = new ButtonSetting("Only while visible", false));
        this.registerSetting(renderOutline = new ButtonSetting("Render block outline", true));
        this.registerSetting(sendAnimations = new ButtonSetting("Send animations", false));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    @Override
    public void onDisable() {
        reset(true, true);
        previousBlockBroken = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) // takes priority over ka & antifireball
    public void onPreUpdate(PreUpdateEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (ModuleManager.bedwars != null && ModuleManager.bedwars.isEnabled() && BedWars.whitelistOwnBed.isToggled() && !BedWars.outsideSpawn) {
            reset(true, true);
            return;
        }
        if (Utils.isBedwarsPracticeOrReplay()) {
            return;
        }
        if (!mc.thePlayer.capabilities.allowEdit || mc.thePlayer.isSpectator()) {
            reset(true, true);
            return;
        }
        if (bedPos == null) {
            if (System.currentTimeMillis() - lastCheck >= rate.getInput() * 1000) {
                lastCheck = System.currentTimeMillis();
                bedPos = getBedPos();
            }
            if (bedPos == null) {
                reset(true, true);
                return;
            }
        }
        else {
            if (!(BlockUtils.getBlock(bedPos[0]) instanceof BlockBed) || (currentBlock != null && BlockUtils.replaceable(currentBlock))) {
                reset(true, true);
                return;
            }
        }
        switch (noAutoBlockTicks) {
            case -1:
                noAutoBlockTicks = -2;
                return;
            case -2:
                resetSlot();
                noAutoBlockTicks = -3;
                return;
            case -3:
                noAutoBlockTicks = -4;
                return;
            case -4:
                stopAutoblock = false;
                noAutoBlockTicks = 0;
                return;
        }
        if (breakNearBlock.isToggled() && isCovered(bedPos[0]) && isCovered(bedPos[1])) {
            if (nearestBlock == null) {
                nearestBlock = getBestBlock(bedPos, true);
            }
            breakBlock(nearestBlock);
        }
        else {
            nearestBlock = null;
            breakBlock(getBestBlock(bedPos, false) != null ? getBestBlock(bedPos, false) : bedPos[0]);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || !cancelKnockback.isToggled() || currentBlock == null) {
            return;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                e.setCanceled(true);
            }
        }
        else if (e.getPacket() instanceof S27PacketExplosion) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        aiming = false;
        if ((rotate || breakProgress >= 1 || breakProgress == 0) && (currentBlock != null || rotateLastBlock != null)) {
            float[] rotations = RotationUtils.getRotations(currentBlock == null ? rotateLastBlock : currentBlock, e.getYaw(), e.getPitch());
            if (currentBlock != null && !RotationUtils.inRange(currentBlock, range.getInput())) {
                return;
            }
            e.setYaw(RotationUtils.applyVanilla(rotations[0]));
            e.setPitch(rotations[1]);
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7rotating (&3" + mc.thePlayer.ticksExisted + "&7).");
            }
            rotate = false;
            if (groundSpoof.isToggled() && !mc.thePlayer.isInWater()) {
                e.setOnGround(true);
            }
            aiming = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent renderWorldLastEvent) {
        if (!renderOutline.isToggled() || currentBlock == null || !Utils.nullCheck()) {
            return;
        }
        if (ModuleManager.bedESP != null && ModuleManager.bedESP.isEnabled()) {
            outlineColor = Theme.getGradient((int) ModuleManager.bedESP.theme.getInput(), 0);
        }
        else if (ModuleManager.hud != null && ModuleManager.hud.isEnabled()) {
            outlineColor = Theme.getGradient((int) ModuleManager.hud.theme.getInput(), 0);
        }
        else {
            outlineColor = defaultOutlineColor;
        }
        RenderUtils.renderBlock(currentBlock, outlineColor, (Arrays.asList(bedPos).contains(currentBlock) ? 0.5625 : 1),true, false);
    }

    private void resetSlot() {
        if (Raven.packetsHandler != null && Raven.packetsHandler.playerSlot != null && Utils.nullCheck() && Raven.packetsHandler.playerSlot.get() != mc.thePlayer.inventory.currentItem && mode.getInput() == 2) {
            setPacketSlot(mc.thePlayer.inventory.currentItem);
        }
        else if (lastSlot != -1) {
            lastSlot = mc.thePlayer.inventory.currentItem = lastSlot;
        }
    }

    public boolean cancelKnockback() {
        return cancelKnockback.isToggled() && currentBlock != null && RotationUtils.inRange(currentBlock, range.getInput());
    }

    private BlockPos[] getBedPos() {
        int range;
        priority:
        for (int n = range = (int) this.range.getInput(); range >= -n; --range) {
            for (int j = -n; j <= n; ++j) {
                for (int k = -n; k <= n; ++k) {
                    final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + j, mc.thePlayer.posY + range, mc.thePlayer.posZ + k);
                    final IBlockState getBlockState = mc.theWorld.getBlockState(blockPos);
                    if (getBlockState.getBlock() == Blocks.bed && getBlockState.getValue((IProperty) BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                        float fov = (float) this.fov.getInput();
                        if (fov != 360 && !Utils.inFov(fov, blockPos)) {
                            continue priority;
                        }
                        return new BlockPos[]{blockPos, blockPos.offset((EnumFacing) getBlockState.getValue((IProperty) BlockBed.FACING))};
                    }
                }
            }
        }
        return null;
    }

    public BlockPos getBestBlock(BlockPos[] positions, boolean getSurrounding) {
        if (positions == null || positions.length == 0) {
            return null;
        }
        HashMap<BlockPos, double[]> blockMap = new HashMap<>();
        for (BlockPos pos : positions) {
            if (pos == null) {
                continue;
            }
            if (getSurrounding) {
                for (EnumFacing enumFacing : EnumFacing.values()) {
                    if (enumFacing == EnumFacing.DOWN) {
                        continue;
                    }
                    BlockPos offset = pos.offset(enumFacing);
                    if (Arrays.asList(positions).contains(offset)) {
                        continue;
                    }
                    if (!RotationUtils.inRange(offset, range.getInput())) {
                        continue;
                    }
                    double efficiency = getEfficiency(offset);
                    double distance = mc.thePlayer.getDistanceSqToCenter(offset);
                    blockMap.put(offset, new double[]{distance, efficiency});
                }
            }
            else {
                if (!RotationUtils.inRange(pos, range.getInput())) {
                    continue;
                }
                double efficiency = getEfficiency(pos);
                double distance = mc.thePlayer.getDistanceSqToCenter(pos);
                blockMap.put(pos, new double[]{distance, efficiency});
            }
        }
        List<Map.Entry<BlockPos, double[]>> sortedByDistance = sortByDistance(blockMap);
        List<Map.Entry<BlockPos, double[]>> sortedByEfficiency = sortByEfficiency(sortedByDistance);
        List<Map.Entry<BlockPos, double[]>> sortedByPreviousBlocks = sortByPreviousBlocks(sortedByEfficiency);
        return sortedByPreviousBlocks.isEmpty() ? null : sortedByPreviousBlocks.get(0).getKey();
    }

    private List<Map.Entry<BlockPos, double[]>> sortByDistance(HashMap<BlockPos, double[]> blockMap) {
        List<Map.Entry<BlockPos, double[]>> list = new ArrayList<>(blockMap.entrySet());
        list.sort(Comparator.comparingDouble(entry -> entry.getValue()[0]));
        return list;
    }

    private List<Map.Entry<BlockPos, double[]>> sortByEfficiency(List<Map.Entry<BlockPos, double[]>> blockList) {
        blockList.sort((entry1, entry2) -> Double.compare(entry2.getValue()[1], entry1.getValue()[1]));
        return blockList;
    }

    private List<Map.Entry<BlockPos, double[]>> sortByPreviousBlocks(List<Map.Entry<BlockPos, double[]>> blockList) {
        blockList.sort((entry1, entry2) -> {
            boolean isEntry1Previous = entry1.getKey().equals(previousBlockBroken);
            boolean isEntry2Previous = entry2.getKey().equals(previousBlockBroken);
            if (isEntry1Previous && !isEntry2Previous) {
                return -1;
            }
            if (!isEntry1Previous && isEntry2Previous) {
                return 1;
            }
            return 0;
        });
        return blockList;
    }

    private double getEfficiency(BlockPos pos) {
        Block block = BlockUtils.getBlock(pos);
        ItemStack tool = (mode.getInput() == 2 && Utils.getTool(block) != -1) ? mc.thePlayer.inventory.getStackInSlot(Utils.getTool(block)) : mc.thePlayer.getHeldItem();
        double efficiency = BlockUtils.getBlockHardness(block, tool, false, ignoreSlow.isToggled() || groundSpoof.isToggled());

        if (breakProgressMap.get(pos) != null) {
            efficiency = breakProgressMap.get(pos);
        }

        return efficiency;
    }

    private void reset(boolean resetSlot, boolean stopAutoblock) {
        if (resetSlot) {
            resetSlot();
        }
        bedPos = null;
        breakProgress = 0;
        rotate = false;
        nearestBlock = null;
        aiming = false;
        currentBlock = null;
        breakProgressMap.clear();
        lastSlot = -1;
        vanillaProgress = 0;
        lastProgress = 0;
        if (stopAutoblock) {
            this.stopAutoblock = false;
            noAutoBlockTicks = 0;
        }
        rotateLastBlock = null;
    }

    public void setPacketSlot(int slot) {
        if (slot == -1) {
            return;
        }
        Raven.packetsHandler.updateSlot(slot);
    }

    private void startBreak(BlockPos blockPos) {
        if (Raven.debug) {
            Utils.sendModuleMessage(this, "sending c07 &astart &7break &7(&b" + mc.thePlayer.ticksExisted + "&7)");
        }
        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, EnumFacing.UP));
    }

    private void stopBreak(BlockPos blockPos) {
        if (Raven.debug) {
            Utils.sendModuleMessage(this, "sending c07 &cstop &7break &7(&b" + mc.thePlayer.ticksExisted + "&7)");
        }
        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, EnumFacing.UP));
    }

    private void swing() {
        mc.thePlayer.swingItem();
    }

    private void breakBlock(BlockPos blockPos) {
        if (blockPos == null) {
            return;
        }
        float fov = (float) this.fov.getInput();
        if (fov != 360 && !Utils.inFov(fov, blockPos)) {
            return;
        }
        if (!RotationUtils.inRange(blockPos, range.getInput())) {
            return;
        }
        if (onlyWhileVisible.isToggled() && (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mc.objectMouseOver.getBlockPos().equals(blockPos))) {
            return;
        }
        if (BlockUtils.replaceable(currentBlock == null ? blockPos : currentBlock)) {
            reset(true, true);
            return;
        }
        currentBlock = blockPos;
        Block block = BlockUtils.getBlock(blockPos);
        if (!silentSwing.isToggled()) {
            swing();
        }
        if (breakProgress == 0 && !aiming) {
            return;
        }
        if ((!stopAutoblock || noAutoBlockTicks == 99) && breakProgress <= 0 && mode.getInput() == 2 && ModuleManager.killAura.autoBlockOverride()) {
            stopAutoblock = true;
            if (noAutoBlockTicks == 0) {
                noAutoBlockTicks = 99;
            }
            else if (noAutoBlockTicks == 99) {
                noAutoBlockTicks = 0;
            }
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7stopping autoblock on &3start &7(&b" + mc.thePlayer.ticksExisted + "&7)");
            }
            return;
        }
        if (mode.getInput() == 2 || mode.getInput() == 0) {
            if (breakProgress == 0 && aiming) {
                resetSlot();
                rotate = true;
                if (mode.getInput() == 0) {
                    setSlot(Utils.getTool(block));
                }
                startBreak(blockPos);
                if (mode.getInput() == 2) {
                    noAutoBlockTicks = 1; // increment by 1
                }
            }
            else if (breakProgress >= 1 && aiming) {
                if (mode.getInput() == 2) {
                    noAutoBlockTicks = -1; // set to -1 to indicate it was on stop rather than +1
                    setPacketSlot(Utils.getTool(block));
                }
                stopBreak(blockPos);
                previousBlockBroken = currentBlock;
                reset(false, false);
                Iterator<Map.Entry<BlockPos, Float>> iterator = breakProgressMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<BlockPos, Float> entry = iterator.next();
                    if (entry.getKey().equals(blockPos)) {
                        iterator.remove();
                    }
                }
                if (!disableBreakEffects.isToggled()) {
                    mc.playerController.onPlayerDestroyBlock(blockPos, EnumFacing.UP);
                }
                rotate = true;
                rotateLastBlock = previousBlockBroken;
                return;
            }
            else {
                if (mode.getInput() == 0) {
                    rotate = true;
                }
                if (noAutoBlockTicks == 1) {
                    stopAutoblock = false;
                    noAutoBlockTicks = 0;
                }
            }
            double progress = vanillaProgress = (float) (BlockUtils.getBlockHardness(block, (mode.getInput() == 2 && Utils.getTool(block) != -1) ? mc.thePlayer.inventory.getStackInSlot(Utils.getTool(block)) : mc.thePlayer.getHeldItem(), false, ignoreSlow.isToggled() || groundSpoof.isToggled()) * breakSpeed.getInput());
            if (lastProgress != 0 && breakProgress >= lastProgress - vanillaProgress) {
                // tick before we break so here we've gotta stop autoblocking
                if (mode.getInput() == 2 && ModuleManager.killAura.autoBlockOverride()) {
                    if (Raven.debug) {
                        Utils.sendModuleMessage(this, "&7stopping autoblock &7(&b" + mc.thePlayer.ticksExisted + "&7)");
                    }
                    stopAutoblock = true; // if blocking then return and stop autoblocking
                }
                if (breakProgress >= lastProgress) {
                    rotate = true;
                }
            }
            breakProgress += progress;
            breakProgressMap.put(blockPos, breakProgress);
            if (sendAnimations.isToggled()) {
                mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, (int) ((breakProgress * 10) - 1));
            }
            lastProgress = 0;
            while (lastProgress + progress < 1) {
                lastProgress += progress;
            }
        }
        else if (mode.getInput() == 1 && aiming) {
            rotate = true;
            if (!silentSwing.isToggled()) {
                swing();
            }
            startBreak(blockPos);
            setSlot(Utils.getTool(block));
            stopBreak(blockPos);
        }
        aiming = false;
    }

    private void setSlot(int slot) {
        if (slot == -1 || slot == mc.thePlayer.inventory.currentItem) {
            return;
        }
        if (lastSlot == -1) {
            lastSlot = mc.thePlayer.inventory.currentItem;
        }
        mc.thePlayer.inventory.currentItem = slot;
    }

    private boolean isCovered(BlockPos blockPos) {
        for (EnumFacing enumFacing : EnumFacing.values()) {
            BlockPos offset = blockPos.offset(enumFacing);
            if (BlockUtils.replaceable(offset) || BlockUtils.notFull(BlockUtils.getBlock(offset)) ) {
                return false;
            }
        }
        return true;
    }
}