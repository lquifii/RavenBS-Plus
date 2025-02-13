package keystrokesmod.module.impl.render;

import keystrokesmod.Raven;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerESP extends Module {
    public SliderSetting red;
    public SliderSetting green;
    public SliderSetting blue;
    public ButtonSetting teamColor;
    public ButtonSetting rainbow;
    public GroupSetting espTypes;
    private ButtonSetting twoD;
    private ButtonSetting box;
    private ButtonSetting healthBar;
    public ButtonSetting outline;
    private ButtonSetting shaded;
    private ButtonSetting skeleton;
    private ButtonSetting ring;
    public ButtonSetting redOnDamage;
    public ButtonSetting renderSelf;
    public ButtonSetting showInvis;

    private int rgb_c = 0;
    private static final float RAD_TO_DEG = 57.29578f;

    private Map<EntityLivingBase, Integer> renderAsTwoD = new HashMap<>(); // entity with its rgb
    // none, outline, box, shaded, 2d, ring

    public PlayerESP() {
        super("PlayerESP", category.render, 0);
        this.registerSetting(espTypes = new GroupSetting("Types"));
        this.registerSetting(twoD = new ButtonSetting(espTypes, "2D", false));
        this.registerSetting(box = new ButtonSetting(espTypes, "Box", false));
        this.registerSetting(outline = new ButtonSetting(espTypes, "Outline", false));
        this.registerSetting(ring = new ButtonSetting(espTypes, "Ring", false));
        this.registerSetting(shaded = new ButtonSetting(espTypes, "Shaded", false));
        this.registerSetting(skeleton = new ButtonSetting(espTypes, "Skeleton", false));
        this.registerSetting(red = new SliderSetting("Red", 0.0D, 0.0D, 255.0D, 1.0D));
        this.registerSetting(green = new SliderSetting("Green", 255.0D, 0.0D, 255.0D, 1.0D));
        this.registerSetting(blue = new SliderSetting("Blue", 0.0D, 0.0D, 255.0D, 1.0D));
        this.registerSetting(rainbow = new ButtonSetting("Rainbow", false));
        this.registerSetting(healthBar = new ButtonSetting("Health bar", true));
        this.registerSetting(redOnDamage = new ButtonSetting("Red on damage", true));
        this.registerSetting(renderSelf = new ButtonSetting("Render self", false));
        this.registerSetting(teamColor = new ButtonSetting("Team color", false));
        this.registerSetting(showInvis = new ButtonSetting("Show invis", true));
    }

    public void guiUpdate() {
        this.rgb_c = (new Color((int) red.getInput(), (int) green.getInput(), (int) blue.getInput())).getRGB();
    }

    @SubscribeEvent
    public void onRenderPlayerEvent(RenderPlayerEvent.Post e) {
        if (skeleton.isToggled() && e.entityPlayer != null && Utils.nullCheck()) {
            EntityPlayer player = e.entityPlayer;
            if (player != mc.thePlayer || (renderSelf.isToggled() && mc.gameSettings.thirdPersonView > 0)) {
                if (player.deathTime != 0) {
                    return;
                }
                if (!showInvis.isToggled() && player.isInvisible()) {
                    return;
                }
                if (mc.thePlayer != player && AntiBot.isBot(player)) {
                    return;
                }
                int rgb = rainbow.isToggled() ? Utils.getChroma(2L, 0L) : this.rgb_c;
                if (teamColor.isToggled()) {
                    rgb = Utils.getColorFromEntity(player);
                }
                this.renderSkeleton(e.entityPlayer, e.renderer.getMainModel(), rgb, e.partialRenderTick);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        this.renderAsTwoD.clear();
        if (Utils.nullCheck()) {
            int rgb = rainbow.isToggled() ? Utils.getChroma(2L, 0L) : this.rgb_c;
            if (Raven.debug) {
                for (final Entity entity : mc.theWorld.loadedEntityList) {
                    if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                        if (teamColor.isToggled()) {
                            rgb = Utils.getColorFromEntity(entity);
                        }
                        rgb = Utils.mergeAlpha(rgb, 255);
                        this.render(entity, rgb);
                        this.renderAsTwoD.put((EntityLivingBase) entity, rgb);
                    }
                }
                return;
            }
            EntityPlayer selfPlayer = (Freecam.freeEntity == null) ? mc.thePlayer : Freecam.freeEntity;
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player != selfPlayer || (renderSelf.isToggled() && mc.gameSettings.thirdPersonView > 0)) {
                    if (player.deathTime != 0) {
                        continue;
                    }
                    if (!showInvis.isToggled() && player.isInvisible()) {
                        continue;
                    }
                    if (selfPlayer != player && AntiBot.isBot(player)) {
                        continue;
                    }
                    if (teamColor.isToggled()) {
                        rgb = Utils.getColorFromEntity(player);
                    }
                    rgb = Utils.mergeAlpha(rgb, 255);
                    this.render(player, rgb);
                    this.renderAsTwoD.put(player, rgb);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTwo2D(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !twoD.isToggled()) {
            return;
        }
        for (Map.Entry<EntityLivingBase, Integer> entry : renderAsTwoD.entrySet()) {
            this.renderTwoD(entry.getKey(), entry.getValue(), 0, e.partialTicks);
        }
    }

    private void render(Entity en, int rgb) {
        if (box.isToggled()) {
            RenderUtils.renderEntity(en, 1, 0, 0, rgb, redOnDamage.isToggled());
        }

        if (shaded.isToggled()) {
            if (ModuleManager.murderMystery == null || !ModuleManager.murderMystery.isEnabled() || ModuleManager.murderMystery.isEmpty()) {
                RenderUtils.renderEntity(en, 2, 0, 0, rgb, redOnDamage.isToggled());
            }
        }

        if (healthBar.isToggled()) {
            RenderUtils.renderEntity(en, 4, 0, 0, rgb, redOnDamage.isToggled());
        }

        if (ring.isToggled()) {
            RenderUtils.renderEntity(en, 6, 0, 0, rgb, redOnDamage.isToggled());
        }
    }

    public void renderTwoD(EntityLivingBase en, int rgb, double expand, float partialTicks) {
        if (!RenderUtils.isInViewFrustum(en)) {
            return;
        }
        ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft) mc).getTimer().renderPartialTicks, 0);

        ScaledResolution scaledResolution = new ScaledResolution(mc);

        double playerX = en.lastTickPosX + (en.posX - en.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double playerY = en.lastTickPosY + (en.posY - en.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double playerZ = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bbox = en.getEntityBoundingBox().expand(0.1D + expand, 0.1D + expand, 0.1D + expand);
        AxisAlignedBB axis = new AxisAlignedBB(
                bbox.minX - en.posX + playerX,
                bbox.minY - en.posY + playerY,
                bbox.minZ - en.posZ + playerZ,
                bbox.maxX - en.posX + playerX,
                bbox.maxY - en.posY + playerY,
                bbox.maxZ - en.posZ + playerZ
        );

        Vec3[] corners = new Vec3[8];
        corners[0] = new Vec3(axis.minX, axis.minY, axis.minZ);
        corners[1] = new Vec3(axis.minX, axis.minY, axis.maxZ);
        corners[2] = new Vec3(axis.minX, axis.maxY, axis.minZ);
        corners[3] = new Vec3(axis.minX, axis.maxY, axis.maxZ);
        corners[4] = new Vec3(axis.maxX, axis.minY, axis.minZ);
        corners[5] = new Vec3(axis.maxX, axis.minY, axis.maxZ);
        corners[6] = new Vec3(axis.maxX, axis.maxY, axis.minZ);
        corners[7] = new Vec3(axis.maxX, axis.maxY, axis.maxZ);

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        boolean isInView = false;

        for (Vec3 corner : corners) {
            double x = corner.xCoord;
            double y = corner.yCoord;
            double z = corner.zCoord;

            Vec3 screenVec = RenderUtils.convertTo2D(scaledResolution.getScaleFactor(), x, y, z);
            if (screenVec != null) {
                if (screenVec.zCoord >= 1.0003684 || screenVec.zCoord <= 0) {
                    continue;
                }

                isInView = true;

                double screenX = screenVec.xCoord;
                double screenY = screenVec.yCoord;

                if (screenX < minX) minX = screenX;
                if (screenY < minY) minY = screenY;
                if (screenX > maxX) maxX = screenX;
                if (screenY > maxY) maxY = screenY;
            }
        }

        if (!isInView) {
            return;
        }

        mc.entityRenderer.setupOverlayRendering();

        ScaledResolution res = new ScaledResolution(mc);
        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();

        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min(screenWidth, maxX);
        maxY = Math.min(screenHeight, maxY);

        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = ( rgb & 0xFF) / 255.0F;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0F);

        // background outline
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(minX, minY);
        GL11.glVertex2d(maxX, minY);
        GL11.glVertex2d(maxX, maxY);
        GL11.glVertex2d(minX, maxY);
        GL11.glEnd();

        // second background
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(minX + 1.0, minY + 1.0);
        GL11.glVertex2d(maxX - 1.0, minY + 1.0);
        GL11.glVertex2d(maxX - 1.0, maxY - 1.0);
        GL11.glVertex2d(minX + 1.0, maxY - 1.0);
        GL11.glEnd();

        // main outline
        GL11.glColor4f(red, green, blue, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(minX + 0.5, minY + 0.5);
        GL11.glVertex2d(maxX - 0.5, minY + 0.5);
        GL11.glVertex2d(maxX - 0.5, maxY - 0.5);
        GL11.glVertex2d(minX + 0.5, maxY - 0.5);
        GL11.glEnd();

        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glPopMatrix();
    }

    public void renderSkeleton(EntityPlayer player, ModelBiped modelBiped, int color, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        double viewerPosX = mc.getRenderManager().viewerPosX;
        double viewerPosY = mc.getRenderManager().viewerPosY;
        double viewerPosZ = mc.getRenderManager().viewerPosZ;

        double posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - viewerPosX;
        double posY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - viewerPosY;
        double posZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - viewerPosZ;

        boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glPushMatrix();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (!wasBlendEnabled) {
            GL11.glEnable(GL11.GL_BLEND);
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f((float) (color >> 16 & 0xFF) / 255.0f, (float) (color >> 8 & 0xFF) / 255.0f, (float) (color & 0xFF) / 255.0f, 1.0f);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glTranslated(posX, posY, posZ);

        float distance = mc.thePlayer.getDistanceToEntity(player);
        float computedLineWidth = 4.0f * ((100.0f - Math.min(distance, 100.0f)) / 100.0f);
        float lineWidth = Math.max(1.0f, computedLineWidth);
        GL11.glLineWidth(lineWidth);


        boolean isSneaking = player.isSneaking();
        float legHeight = isSneaking ? 0.6f : 0.75f;
        double legOffsetZ = isSneaking ? -0.2 : 0.0;

        GL11.glRotatef(player.renderYawOffset, 0.0f, -999.0f, 0.0f);
        GL11.glTranslated(-0.15, legHeight, legOffsetZ);

        // Render the right leg
        float rightLegRotX = modelBiped.bipedRightLeg.rotateAngleX * RAD_TO_DEG;
        float rightLegRotY = modelBiped.bipedRightLeg.rotateAngleY * RAD_TO_DEG;
        float rightLegRotZ = modelBiped.bipedRightLeg.rotateAngleZ * RAD_TO_DEG;
        GL11.glRotatef(rightLegRotX, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(-rightLegRotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-rightLegRotZ, 0.0f, 0.0f, 1.0f);
        drawLine(0.0, 0.0, 0.0, 0.0, -legHeight, 0.0);
        // Undo the right leg rotations.
        GL11.glRotatef(rightLegRotZ, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(rightLegRotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-rightLegRotX, 1.0f, 0.0f, 0.0f);

        // Render left leg
        GL11.glTranslated(0.3, 0.0, 0.0);
        float leftLegRotX = modelBiped.bipedLeftLeg.rotateAngleX * RAD_TO_DEG;
        float leftLegRotY = modelBiped.bipedLeftLeg.rotateAngleY * RAD_TO_DEG;
        float leftLegRotZ = modelBiped.bipedLeftLeg.rotateAngleZ * RAD_TO_DEG;
        GL11.glRotatef(leftLegRotX, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(-leftLegRotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-leftLegRotZ, 0.0f, 0.0f, 1.0f);
        drawLine(0.0, 0.0, 0.0, 0.0, -legHeight, 0.0);
        // Undo the left leg rotations.
        GL11.glRotatef(leftLegRotZ, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(leftLegRotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-leftLegRotX, 1.0f, 0.0f, 0.0f);
        GL11.glTranslated(-0.15, 0.0, 0.0);

        // Draw a line connecting the legs.
        drawLine(0.15, 0.0, 0.0, -0.15, 0.0, 0.0);

        // Renders the torso
        if (player.isSneaking()) {
            GL11.glRotatef(20.0f, 1.0f, 0.0f, 0.0f);
        }
        drawLine(0.0, 0.0, 0.0, 0.0, 0.65, 0.0);

        // Move to the top of the torso (shoulder level) and draw shoulders.
        GL11.glTranslated(0.0, 0.65, 0.0);
        drawLine(0.35, 0.0, 0.0, -0.35, 0.0, 0.0);
        GL11.glTranslated(-0.35, 0.0, 0.0);

        // Render right arm
        float rightArmRotX = modelBiped.bipedRightArm.rotateAngleX * RAD_TO_DEG;
        float rightArmRotY = modelBiped.bipedRightArm.rotateAngleY * RAD_TO_DEG;
        float rightArmRotZ = modelBiped.bipedRightArm.rotateAngleZ * RAD_TO_DEG;
        GL11.glRotatef(rightArmRotX, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(-rightArmRotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-rightArmRotZ, 0.0f, 0.0f, 1.0f);
        drawLine(0.0, 0.0, 0.0, 0.0, -0.6, 0.0);
        // Undo the right arm rotations.
        GL11.glRotatef(rightArmRotZ, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(rightArmRotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-rightArmRotX, 1.0f, 0.0f, 0.0f);

        // Render left arm
        GL11.glTranslated(0.7, 0.0, 0.0);
        float leftArmRotX = modelBiped.bipedLeftArm.rotateAngleX * RAD_TO_DEG;
        float leftArmRotY = modelBiped.bipedLeftArm.rotateAngleY * RAD_TO_DEG;
        float leftArmRotZ = modelBiped.bipedLeftArm.rotateAngleZ * RAD_TO_DEG;
        GL11.glRotatef(leftArmRotX, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(-leftArmRotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-leftArmRotZ, 0.0f, 0.0f, 1.0f);
        drawLine(0.0, 0.0, 0.0, 0.0, -0.6, 0.0);
        // Undo the left arm rotations.
        GL11.glRotatef(leftArmRotZ, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(leftArmRotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-leftArmRotX, 1.0f, 0.0f, 0.0f);
        GL11.glTranslated(-0.35, 0.0, 0.0);

        // renders head
        // undo the torso rotation.
        GL11.glRotatef(-player.renderYawOffset, 0.0f, -999.0f, 0.0f);
        double headHeight = 0.4;
        GL11.glRotated(player.rotationYaw, 0.0, -999.0, 0.0);
        GL11.glRotated(player.rotationPitch, 999.0, 0.0, 0.0);
        drawLine(0.0, 0.0, 0.0, 0.0, headHeight, 0.0);
        drawLine(0.0, headHeight, 0.0, 0.0, headHeight, 0.25);
        GL11.glRotated(player.rotationPitch, 999.0, 0.0, 0.0);
        GL11.glRotated(-player.rotationYaw, 0.0, 999.0, 0.0);

        if (!wasBlendEnabled) {
            GL11.glDisable(GL11.GL_BLEND);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();

        GL11.glColor4f(1, 1, 1,1);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glEnd();
    }
}