/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.EnumParticleTypes;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * HitFX - LiquidBounce style hit effects
 * Particle effects and custom sounds on hit
 */
public class HitFX extends Module {

    // Particle settings
    private final ModeSetting particleMode;
    private final SliderSetting particleAmount;

    // Sound settings
    private final TickSetting soundEnabled;
    private final ModeSetting soundMode;
    private final SliderSetting soundVolume;

    // Particle types
    private static final String[] PARTICLES = {"Blood", "Fire", "Heart", "Water", "Smoke", "Magic", "Crits", "None"};

    // Sound types - using Effects sounds
    private static final String[] SOUNDS = {"Arena Switch", "Cod", "Crystal", "Duck"};
    private static final String[] SOUND_FILES = {
        "/hitsounds/Arena Switch.wav",
        "/hitsounds/Cod.wav",
        "/hitsounds/Crystal.wav",
        "/hitsounds/Duck.wav"
    };

    // State tracking
    private final Map<Integer, Integer> previousHurtTimes = new HashMap<>();
    private final Map<Integer, Long> attackedEntities = new HashMap<>();
    private static final long ATTACK_TIMEOUT = 1000;
    private final Random random = new Random();

    public HitFX() {
        super("HitFX", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("LiquidBounce style hit effects"));

        // Particle settings
        this.registerSetting(new DescriptionSetting("--- Particles ---"));
        this.registerSetting(particleMode = new ModeSetting("Particle", PARTICLES));
        this.registerSetting(particleAmount = new SliderSetting("Amount", 5, 1, 20, 1));

        // Sound settings
        this.registerSetting(new DescriptionSetting("--- Sound ---"));
        this.registerSetting(soundEnabled = new TickSetting("Sound", true));
        this.registerSetting(soundMode = new ModeSetting("Sound Type", SOUNDS));
        this.registerSetting(soundVolume = new SliderSetting("Volume", 100, 0, 100, 5));
    }

    @Override
    public void onEnable() {
        previousHurtTimes.clear();
        attackedEntities.clear();
    }

    @Override
    public void onDisable() {
        previousHurtTimes.clear();
        attackedEntities.clear();
    }

    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled) return true;

        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;

            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                if (mc.theWorld != null) {
                    Entity target = useEntity.getEntityFromWorld(mc.theWorld);
                    if (target != null && target instanceof EntityLivingBase && target != mc.thePlayer) {
                        attackedEntities.put(target.getEntityId(), System.currentTimeMillis());
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        long currentTime = System.currentTimeMillis();

        // Clean up old attack records
        attackedEntities.entrySet().removeIf(entry -> currentTime - entry.getValue() > ATTACK_TIMEOUT * 2);

        // Check all living entities
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue;
            if (!(entity instanceof EntityLivingBase)) continue;

            // Skip bots
            if (AntiBot.isBotForVisuals(entity)) continue;

            EntityLivingBase living = (EntityLivingBase) entity;
            int entityId = entity.getEntityId();

            // Hit detection
            int currentHurtTime = living.hurtTime;
            int prevHurtTime = previousHurtTimes.getOrDefault(entityId, 0);

            // Detect hit: hurtTime goes from low to high (9-10 means just got hit)
            if (currentHurtTime > prevHurtTime && currentHurtTime >= 9) {
                // Only trigger if WE attacked this entity recently
                Long attackTime = attackedEntities.get(entityId);
                if (attackTime != null && currentTime - attackTime < ATTACK_TIMEOUT) {
                    onHit(living);
                }
            }

            previousHurtTimes.put(entityId, currentHurtTime);
        }
    }

    /**
     * Called when we hit an entity
     */
    private void onHit(EntityLivingBase entity) {
        // Spawn particles
        spawnParticles(entity);

        // Play sound
        if (soundEnabled.isEnabled()) {
            playHitSound();
        }
    }

    /**
     * Spawn particles on hit
     */
    private void spawnParticles(EntityLivingBase entity) {
        if (mc.theWorld == null) return;

        String mode = particleMode.getSelected();
        if (mode.equals("None")) return;

        int amount = (int) particleAmount.getValue();
        double x = entity.posX;
        double y = entity.posY + entity.height / 2;
        double z = entity.posZ;

        for (int i = 0; i < amount; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 1.0;
            double offsetY = (random.nextDouble() - 0.5) * entity.height;
            double offsetZ = (random.nextDouble() - 0.5) * 1.0;

            double velX = (random.nextDouble() - 0.5) * 0.3;
            double velY = random.nextDouble() * 0.2;
            double velZ = (random.nextDouble() - 0.5) * 0.3;

            switch (mode) {
                case "Blood":
                    // Blood particles using redstone block
                    int blockId = Block.getIdFromBlock(Blocks.redstone_block);
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.BLOCK_CRACK,
                        x + offsetX, y + offsetY, z + offsetZ,
                        velX, velY + 0.1, velZ,
                        blockId
                    );
                    break;

                case "Fire":
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.FLAME,
                        x + offsetX, y + offsetY, z + offsetZ,
                        velX, velY, velZ
                    );
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.LAVA,
                        x + offsetX, y + offsetY, z + offsetZ,
                        0, 0, 0
                    );
                    break;

                case "Heart":
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.HEART,
                        x + offsetX, y + offsetY + 0.5, z + offsetZ,
                        velX * 0.5, velY, velZ * 0.5
                    );
                    break;

                case "Water":
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.WATER_SPLASH,
                        x + offsetX, y + offsetY, z + offsetZ,
                        velX, velY, velZ
                    );
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.DRIP_WATER,
                        x + offsetX, y + offsetY + 0.3, z + offsetZ,
                        0, 0, 0
                    );
                    break;

                case "Smoke":
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.SMOKE_LARGE,
                        x + offsetX, y + offsetY, z + offsetZ,
                        velX * 0.3, velY * 0.3, velZ * 0.3
                    );
                    break;

                case "Magic":
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.ENCHANTMENT_TABLE,
                        x + offsetX, y + offsetY, z + offsetZ,
                        velX, velY, velZ
                    );
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.SPELL_WITCH,
                        x + offsetX, y + offsetY, z + offsetZ,
                        velX * 0.5, velY * 0.5, velZ * 0.5
                    );
                    break;

                case "Crits":
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.CRIT,
                        x + offsetX, y + offsetY, z + offsetZ,
                        velX, velY, velZ
                    );
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.CRIT_MAGIC,
                        x + offsetX, y + offsetY, z + offsetZ,
                        velX, velY, velZ
                    );
                    break;
            }
        }
    }

    /**
     * Play hit sound
     */
    private void playHitSound() {
        new Thread(() -> {
            try {
                int soundIndex = getSoundIndex(soundMode.getSelected());
                String soundFile = SOUND_FILES[soundIndex];

                InputStream is = getClass().getResourceAsStream(soundFile);
                if (is == null) {
                    is = HitFX.class.getResourceAsStream(soundFile);
                }
                if (is == null) return;

                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bis);

                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                // Set volume
                float volume = (float) soundVolume.getValue() / 100f;
                try {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (Math.log10(Math.max(volume, 0.0001)) * 20);
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    gainControl.setValue(dB);
                } catch (Exception ignored) {}

                clip.start();

                // Close clip when done
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private int getSoundIndex(String soundName) {
        for (int i = 0; i < SOUNDS.length; i++) {
            if (SOUNDS[i].equals(soundName)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + particleMode.getSelected();
    }
}
