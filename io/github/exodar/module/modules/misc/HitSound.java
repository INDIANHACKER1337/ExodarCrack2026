/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * HitSound - Plays a sound when an enemy takes damage
 * Sounds are embedded in the JAR
 */
public class HitSound extends Module {

    private static HitSound instance;

    private final ModeSetting sound;
    private final SliderSetting volume;

    // Track previous hurtTime for each entity
    private final Map<Integer, Integer> previousHurtTimes = new HashMap<>();

    // Cached audio clips
    private Clip currentClip = null;
    private String currentSoundName = null;

    // Sound file mappings (only WAV files - MP3 requires additional library)
    private static final String[] SOUNDS = {"Arena Switch", "Cod", "Duck"};
    private static final String[] SOUND_FILES = {
        "/hitsounds/Arena Switch.wav",
        "/hitsounds/Cod.wav",
        "/hitsounds/Duck.wav"
    };

    public HitSound() {
        super("HitSound", ModuleCategory.MISC);
        instance = this;

        this.registerSetting(new DescriptionSetting("Play sound on enemy hit"));
        this.registerSetting(sound = new ModeSetting("Sound", SOUNDS));
        this.registerSetting(volume = new SliderSetting("Volume", 100, 0, 100, 5));
    }

    @Override
    public void onEnable() {
        previousHurtTimes.clear();
    }

    @Override
    public void onDisable() {
        previousHurtTimes.clear();
        if (currentClip != null) {
            currentClip.close();
            currentClip = null;
            currentSoundName = null;
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        // Iterate through all entities
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue;
            if (!(entity instanceof EntityLivingBase)) continue;
            if (!entity.isEntityAlive()) continue;

            // Skip bots
            if (AntiBot.isBotForVisuals(entity)) continue;

            // Only check players (can be expanded to all living entities)
            if (!(entity instanceof EntityPlayer)) continue;

            EntityLivingBase living = (EntityLivingBase) entity;
            int entityId = entity.getEntityId();
            int currentHurtTime = living.hurtTime;
            int prevHurtTime = previousHurtTimes.getOrDefault(entityId, 0);

            // Detect damage: hurtTime goes from 0 to > 0 (max 10)
            // hurtTime = 10 means just got hit, counts down to 0
            if (currentHurtTime > 0 && prevHurtTime == 0) {
                // Entity just took damage - play sound!
                playHitSound();
            }

            previousHurtTimes.put(entityId, currentHurtTime);
        }

        // Clean up old entries for dead/removed entities
        previousHurtTimes.entrySet().removeIf(entry -> {
            Entity entity = mc.theWorld.getEntityByID(entry.getKey());
            return entity == null || !entity.isEntityAlive();
        });
    }

    private void playHitSound() {
        try {
            int soundIndex = getSoundIndex(sound.getSelected());
            String soundFile = SOUND_FILES[soundIndex];

            // Load or reuse clip
            if (currentClip == null || !sound.getSelected().equals(currentSoundName)) {
                if (currentClip != null) {
                    currentClip.stop();
                    currentClip.close();
                }
                currentClip = loadClip(soundFile);
                currentSoundName = sound.getSelected();
            }

            if (currentClip != null) {
                // Set volume
                try {
                    FloatControl gainControl = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
                    float vol = (float) volume.getValue() / 100f;
                    // Convert linear volume to decibels
                    float dB = (float) (Math.log10(Math.max(vol, 0.0001)) * 20);
                    // Clamp to control range
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    gainControl.setValue(dB);
                } catch (Exception ignored) {
                    // Volume control may not be available
                }

                // Stop current playback, reset position, and play again
                // This ensures sound plays even if rapidly triggered
                currentClip.stop();
                currentClip.flush();
                currentClip.setFramePosition(0);
                currentClip.start();
            }
        } catch (Exception e) {
            // Silently fail - don't spam console
        }
    }

    private Clip loadClip(String resourcePath) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                // Try alternative paths
                is = HitSound.class.getResourceAsStream(resourcePath);
            }
            if (is == null) {
                return null;
            }

            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bis);

            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    private int getSoundIndex(String soundName) {
        for (int i = 0; i < SOUNDS.length; i++) {
            if (SOUNDS[i].equals(soundName)) {
                return i;
            }
        }
        return 0;
    }

    public static HitSound getInstance() {
        return instance;
    }

    @Override
    public String getDisplaySuffix() {
        return " \u00a77" + sound.getSelected();
    }
}
