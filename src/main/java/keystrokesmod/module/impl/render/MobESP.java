package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MobESP extends Module {
    private ButtonSetting healthBar;
    private ButtonSetting blaze, creeper, enderman, ghast, silverfish, skeleton, slime, spider, zombie, zombiePigman;

    private final Map<Class<? extends EntityLivingBase>, MobSetting> mobRenders = new HashMap<>();

    public MobESP() {
        super("MobESP", category.render);
        this.registerSetting(healthBar = new ButtonSetting("Health bar", false));
        this.registerSetting(blaze = new ButtonSetting("Blaze §6Orange", true));
        this.registerSetting(creeper = new ButtonSetting("Creeper §aGreen", true));
        this.registerSetting(enderman = new ButtonSetting("Enderman §7Black", true));
        this.registerSetting(ghast = new ButtonSetting("Ghast §fWhite", true));
        this.registerSetting(silverfish = new ButtonSetting("Silverfish §7Gray", true));
        this.registerSetting(skeleton = new ButtonSetting("Skeleton §fWhite", true));
        this.registerSetting(slime = new ButtonSetting("Slime §aGreen", true));
        this.registerSetting(spider = new ButtonSetting("Spider §7Black", true));
        this.registerSetting(zombie = new ButtonSetting("Zombie §1Blue", true));
        this.registerSetting(zombiePigman = new ButtonSetting("Zombie Pigman §dPink", true));

        mobRenders.put(EntityBlaze.class, new MobSetting(blaze, Color.orange.getRGB(), 69.0D));
        mobRenders.put(EntityCreeper.class, new MobSetting(creeper, Color.green.getRGB(), 69.0D));
        mobRenders.put(EntityEnderman.class, new MobSetting(enderman, Color.black.getRGB(), 106.0D));
        mobRenders.put(EntityGhast.class, new MobSetting(ghast, Color.white.getRGB(), 143.0D));
        mobRenders.put(EntitySilverfish.class, new MobSetting(silverfish, Color.gray.getRGB(), 20.0D));
        mobRenders.put(EntitySkeleton.class, new MobSetting(skeleton, Color.white.getRGB(), 69.0D));
        mobRenders.put(EntitySlime.class, new MobSetting(slime, Color.green.getRGB()));
        mobRenders.put(EntitySpider.class, new MobSetting(spider, Color.black.getRGB(), 40.0D));
        mobRenders.put(EntityCaveSpider.class, new MobSetting(spider, Color.black.getRGB(), 26.0D));
        mobRenders.put(EntityZombie.class, new MobSetting(zombie, Color.blue.getRGB()));
        mobRenders.put(EntityPigZombie.class, new MobSetting(zombiePigman, Color.pink.getRGB()));
    }

    private void renderMob(EntityLivingBase entity, double height, int rgb, float partialTicks) {
        RenderUtils.renderEntity(entity, 2, 0.0, 0.0, rgb, false);
        if (healthBar.isToggled()) {
            drawHealthBar(entity, height, partialTicks);
        }
    }

    private void renderEntity(EntityLivingBase entity, float partialTicks) {
        MobSetting mobSetting = mobRenders.get(entity.getClass());
        if (mobSetting != null && mobSetting.setting.isToggled()) {
            renderMob(entity, mobSetting.height, mobSetting.color, partialTicks);
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                if (((EntityLivingBase) entity).deathTime != 0) {
                    continue;
                }
                renderEntity((EntityLivingBase) entity, e.partialTicks);
            }
        }
    }

    private void drawHealthBar(EntityLivingBase en, double mobHeight, float partialTicks) {
        double x = en.lastTickPosX + (en.posX - en.lastTickPosX) * (double) partialTicks - mc.getRenderManager().viewerPosX;
        double y = en.lastTickPosY + (en.posY - en.lastTickPosY) * (double) partialTicks - mc.getRenderManager().viewerPosY;
        double z = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * (double) partialTicks - mc.getRenderManager().viewerPosZ;
        GlStateManager.pushMatrix();
        int xOffset = 21;
        double health = en.getHealth() / en.getMaxHealth();
        int height = (int) (mobHeight * health);
        int healthColor = health < 0.3D ? Color.red.getRGB() : (health < 0.5D ? Color.orange.getRGB() : (health < 0.7D ? Color.yellow.getRGB() : Color.green.getRGB()));
        GL11.glTranslated(x, y - 0.2D, z);
        GL11.glRotated(-mc.getRenderManager().playerViewY, 0.0D, 1.0D, 0.0D);
        GlStateManager.disableDepth();
        GL11.glScalef(0.03F, 0.03F, 0.03F);
        net.minecraft.client.gui.Gui.drawRect(xOffset, -1, xOffset + 4, (int) (mobHeight + 1), Color.black.getRGB());
        net.minecraft.client.gui.Gui.drawRect(xOffset + 1, height, xOffset + 3, (int) mobHeight, Color.darkGray.getRGB());
        net.minecraft.client.gui.Gui.drawRect(xOffset + 1, 0, xOffset + 3, height, healthColor);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private static class MobSetting {
        ButtonSetting setting;
        int color;
        double height = 74.0D;

        public MobSetting(ButtonSetting setting, int color) {
            this.setting = setting;
            this.color = color;
        }

        public MobSetting(ButtonSetting setting, int color, double height) {
            this.setting = setting;
            this.color = color;
            this.height = height;
        }
    }
}