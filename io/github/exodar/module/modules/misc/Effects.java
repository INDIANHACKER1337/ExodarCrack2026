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
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.EnumParticleTypes;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Effects - Visual and audio effects for hits and kills
 */
public class Effects extends Module {

    private static Effects instance;

    // ========== Hit Sound ==========
    private final TickSetting hitSoundEnabled;
    private final ModeSetting hitSound;
    private final SliderSetting hitSoundVolume;

    // ========== Hit Effect ==========
    private final TickSetting hitEffectEnabled;
    private final ModeSetting hitEffect;
    private final SliderSetting hitEffectIntensity;

    // ========== Kill Sound ==========
    private final TickSetting killSoundEnabled;
    private final ModeSetting killSound;
    private final SliderSetting killSoundVolume;

    // ========== Kill Effect ==========
    private final TickSetting killEffectEnabled;
    private final ModeSetting killEffect;

    // ========== Self Hit Sound ==========
    private final TickSetting selfHitSoundEnabled;
    private final ModeSetting selfHitSound;
    private final SliderSetting selfHitSoundVolume;
    private final TickSetting cancelOriginalHurtSound;

    // Sound files - Original + LiquidBounce sounds
    private static final String[] HIT_SOUNDS = {
        "Arena Switch", "Cod", "Crystal", "Duck",
        "Boykisser", "Bonk", "Pop", "UWU", "NYA", "Meow",
        "Click", "Glass", "Soft", "Squash", "MagicSquash",
        "Tung", "Bring", "Moan"
    };
    private static final String[] HIT_SOUND_FILES = {
        "/hitsounds/Arena Switch.wav",
        "/hitsounds/Cod.wav",
        "/hitsounds/Crystal.wav",
        "/hitsounds/Duck.wav",
        "/hitsounds/boykisser.wav",
        "/hitsounds/bonk.wav",
        "/hitsounds/pop.wav",
        "/hitsounds/uwu.wav",
        "/hitsounds/nya.wav",
        "/hitsounds/meow.wav",
        "/hitsounds/click.wav",
        "/hitsounds/glass.wav",
        "/hitsounds/soft.wav",
        "/hitsounds/squash.wav",
        "/hitsounds/magicsquash.wav",
        "/hitsounds/tung.wav",
        "/hitsounds/bring.wav",
        "/hitsounds/moan.wav"
    };

    private static final String[] KILL_SOUNDS = {
        "Arena Switch", "Cod", "Crystal", "Duck",
        "Boykisser", "Bonk", "Pop", "UWU", "NYA", "Meow",
        "Click", "Glass", "Soft", "Squash", "MagicSquash",
        "Tung", "Bring", "Moan"
    };
    private static final String[] KILL_SOUND_FILES = {
        "/hitsounds/Arena Switch.wav",
        "/hitsounds/Cod.wav",
        "/hitsounds/Crystal.wav",
        "/hitsounds/Duck.wav",
        "/hitsounds/boykisser.wav",
        "/hitsounds/bonk.wav",
        "/hitsounds/pop.wav",
        "/hitsounds/uwu.wav",
        "/hitsounds/nya.wav",
        "/hitsounds/meow.wav",
        "/hitsounds/click.wav",
        "/hitsounds/glass.wav",
        "/hitsounds/soft.wav",
        "/hitsounds/squash.wav",
        "/hitsounds/magicsquash.wav",
        "/hitsounds/tung.wav",
        "/hitsounds/bring.wav",
        "/hitsounds/moan.wav"
    };

    // Hit effect types - LiquidBounce style
    private static final String[] HIT_EFFECTS = {"Blood", "Fire", "Heart", "Water", "Smoke", "Magic", "Crits"};

    // Kill effect types
    private static final String[] KILL_EFFECTS = {"Lightning", "Explosion", "Totem"};

    // State tracking
    private int previousSelfHurtTime = 0; // Track player's own hurtTime
    private final Map<Integer, Integer> previousHurtTimes = new HashMap<>();
    private final Map<Integer, Float> previousHealth = new HashMap<>();
    private final Map<Integer, Double[]> lastKnownPositions = new HashMap<>(); // For kill effects
    private final Set<Integer> trackedEntities = new HashSet<>(); // Track which entities we're monitoring

    // Track player attacks via packet - much more reliable than swing detection
    private final Map<Integer, Long> attackedEntities = new HashMap<>(); // entityId -> timestamp of last attack
    private static final long ATTACK_TIMEOUT = 1000; // 1 second window (accounts for server lag)

    // Audio - use byte arrays to reload clip each time for instant replay
    private byte[] hitSoundData = null;
    private byte[] killSoundData = null;
    private String loadedHitSound = null;
    private String loadedKillSound = null;

    public Effects() {
        super("Effects", ModuleCategory.MISC);
        instance = this;

        this.registerSetting(new DescriptionSetting("Visual & audio effects"));

        // Hit Sound settings
        this.registerSetting(new DescriptionSetting("--- Hit Sound ---"));
        this.registerSetting(hitSoundEnabled = new TickSetting("Hit Sound", true));
        this.registerSetting(hitSound = new ModeSetting("Hit Sound Type", HIT_SOUNDS));
        this.registerSetting(hitSoundVolume = new SliderSetting("Hit Sound Volume", 100, 0, 100, 5));

        // Hit Effect settings
        this.registerSetting(new DescriptionSetting("--- Hit Effect ---"));
        this.registerSetting(hitEffectEnabled = new TickSetting("Hit Effect", true));
        this.registerSetting(hitEffect = new ModeSetting("Hit Effect Type", HIT_EFFECTS));
        this.registerSetting(hitEffectIntensity = new SliderSetting("Hit Particles", 10, 1, 30, 1));

        // Kill Sound settings
        this.registerSetting(new DescriptionSetting("--- Kill Sound ---"));
        this.registerSetting(killSoundEnabled = new TickSetting("Kill Sound", true));
        this.registerSetting(killSound = new ModeSetting("Kill Sound Type", KILL_SOUNDS));
        this.registerSetting(killSoundVolume = new SliderSetting("Kill Sound Volume", 100, 0, 100, 5));

        // Kill Effect settings
        this.registerSetting(new DescriptionSetting("--- Kill Effect ---"));
        this.registerSetting(killEffectEnabled = new TickSetting("Kill Effect", true));
        this.registerSetting(killEffect = new ModeSetting("Kill Effect Type", KILL_EFFECTS));

        // Self Hit Sound settings
        this.registerSetting(new DescriptionSetting("--- Self Hit Sound ---"));
        this.registerSetting(selfHitSoundEnabled = new TickSetting("Self Hit Sound", false));
        this.registerSetting(selfHitSound = new ModeSetting("Self Sound Type", HIT_SOUNDS));
        this.registerSetting(selfHitSoundVolume = new SliderSetting("Self Sound Volume", 100, 0, 100, 5));
        this.registerSetting(cancelOriginalHurtSound = new TickSetting("Cancel Original Sound", true));
    }

    @Override
    public void onEnable() {
        previousSelfHurtTime = 0;
        previousHurtTimes.clear();
        previousHealth.clear();
        lastKnownPositions.clear();
        trackedEntities.clear();
        attackedEntities.clear();
    }

    @Override
    public void onDisable() {
        previousSelfHurtTime = 0;
        previousHurtTimes.clear();
        previousHealth.clear();
        lastKnownPositions.clear();
        trackedEntities.clear();
        attackedEntities.clear();
    }

    /**
     * Intercept attack packets to track which entities we attacked
     * This is much more reliable than swing detection
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled) return true;

        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;

            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                // Get the entity we're attacking
                if (mc.theWorld != null) {
                    Entity target = useEntity.getEntityFromWorld(mc.theWorld);
                    if (target != null && target instanceof EntityPlayer && target != mc.thePlayer) {
                        // Record this attack with current timestamp
                        attackedEntities.put(target.getEntityId(), System.currentTimeMillis());
                    }
                }
            }
        }

        return true; // Don't cancel packet
    }

    /**
     * Intercept incoming packets to cancel original hurt sounds
     */
    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;
        if (!selfHitSoundEnabled.isEnabled()) return true;
        if (!cancelOriginalHurtSound.isEnabled()) return true;

        if (packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect soundPacket = (S29PacketSoundEffect) packet;
            String soundName = soundPacket.getSoundName();

            // Cancel player hurt sounds
            if (soundName != null && (
                soundName.contains("hurt") ||
                soundName.contains("damage") ||
                soundName.equals("game.player.hurt") ||
                soundName.equals("random.hurt"))) {

                // Only cancel if sound is near the player
                double dist = mc.thePlayer.getDistanceSq(
                    soundPacket.getX(),
                    soundPacket.getY(),
                    soundPacket.getZ()
                );
                if (dist < 16) { // Within 4 blocks
                    return false; // Cancel packet
                }
            }
        }

        return true;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        // ========== Self Hit Detection ==========
        if (selfHitSoundEnabled.isEnabled()) {
            int currentSelfHurtTime = mc.thePlayer.hurtTime;
            // Detect when player gets hit (hurtTime goes from low to high)
            if (currentSelfHurtTime > previousSelfHurtTime && currentSelfHurtTime >= 9) {
                onSelfHit();
            }
            previousSelfHurtTime = currentSelfHurtTime;
        }

        long currentTime = System.currentTimeMillis();

        // Clean up old attack records (attacks older than 2x timeout)
        attackedEntities.entrySet().removeIf(entry -> currentTime - entry.getValue() > ATTACK_TIMEOUT * 2);

        // Get current entity IDs in world
        Set<Integer> currentEntities = new HashSet<>();

        // Iterate through all players
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue;
            if (!(entity instanceof EntityPlayer)) continue;

            // Skip bots
            if (AntiBot.isBotForVisuals(entity)) continue;

            EntityLivingBase living = (EntityLivingBase) entity;
            int entityId = entity.getEntityId();
            currentEntities.add(entityId);

            // Save position for kill effects
            lastKnownPositions.put(entityId, new Double[]{entity.posX, entity.posY, entity.posZ});

            // ========== Hit Detection ==========
            int currentHurtTime = living.hurtTime;
            int prevHurtTime = previousHurtTimes.getOrDefault(entityId, 0);

            // Detect hit: hurtTime goes from low to high (9-10 means just got hit)
            // hurtTime = 10 when just hit, counts down to 0
            if (currentHurtTime > prevHurtTime && currentHurtTime >= 9) {
                // Only trigger if WE attacked this entity recently (via packet)
                Long attackTime = attackedEntities.get(entityId);
                if (attackTime != null && currentTime - attackTime < ATTACK_TIMEOUT) {
                    onEntityHit(living);
                }
            }

            previousHurtTimes.put(entityId, currentHurtTime);

            // ========== Kill Detection ==========
            float currentHealth = living.getHealth();
            float prevHealth = previousHealth.getOrDefault(entityId, -1f);

            // First time seeing this entity
            if (prevHealth < 0) {
                previousHealth.put(entityId, currentHealth);
                trackedEntities.add(entityId);
                continue;
            }

            // Detect kill: health goes from > 0 to <= 0
            if (prevHealth > 0 && currentHealth <= 0) {
                // Only trigger if WE attacked this entity recently (via packet)
                Long attackTime = attackedEntities.get(entityId);
                if (attackTime != null && currentTime - attackTime < ATTACK_TIMEOUT) {
                    onEntityKilled(living);
                }
            }

            previousHealth.put(entityId, currentHealth);
            trackedEntities.add(entityId);
        }

        // Check for entities that were removed from the world (possibly killed)
        Set<Integer> removedEntities = new HashSet<>(trackedEntities);
        removedEntities.removeAll(currentEntities);

        for (Integer entityId : removedEntities) {
            Float lastHealth = previousHealth.get(entityId);
            Double[] lastPos = lastKnownPositions.get(entityId);

            // If entity was being tracked and had health > 0, it might have been killed
            // This catches cases where entity is removed immediately on death
            if (lastHealth != null && lastHealth > 0 && lastPos != null) {
                // Only trigger if WE attacked this entity recently (via packet)
                Long attackTime = attackedEntities.get(entityId);
                if (attackTime != null && currentTime - attackTime < ATTACK_TIMEOUT) {
                    // Entity disappeared while having health - likely killed by us
                    onEntityKilledAtPosition(lastPos[0], lastPos[1], lastPos[2]);
                }
            }

            // Clean up
            previousHurtTimes.remove(entityId);
            previousHealth.remove(entityId);
            lastKnownPositions.remove(entityId);
            attackedEntities.remove(entityId);
        }

        trackedEntities.clear();
        trackedEntities.addAll(currentEntities);
    }

    /**
     * Called when the player takes damage
     */
    private void onSelfHit() {
        if (selfHitSoundEnabled.isEnabled()) {
            playSound(selfHitSound.getSelected(), HIT_SOUNDS, HIT_SOUND_FILES, (float) selfHitSoundVolume.getValue() / 100f);
        }
    }

    /**
     * Called when an entity is hit (takes damage)
     */
    private void onEntityHit(EntityLivingBase entity) {
        // Hit Sound
        if (hitSoundEnabled.isEnabled()) {
            playSound(hitSound.getSelected(), HIT_SOUNDS, HIT_SOUND_FILES, (float) hitSoundVolume.getValue() / 100f);
        }

        // Hit Effect
        if (hitEffectEnabled.isEnabled()) {
            spawnHitEffect(entity);
        }
    }

    /**
     * Called when an entity is killed
     */
    private void onEntityKilled(EntityLivingBase entity) {
        // Kill Sound
        if (killSoundEnabled.isEnabled()) {
            playSound(killSound.getSelected(), KILL_SOUNDS, KILL_SOUND_FILES, (float) killSoundVolume.getValue() / 100f);
        }

        // Kill Effect
        if (killEffectEnabled.isEnabled()) {
            spawnKillEffect(entity.posX, entity.posY, entity.posZ);
        }
    }

    /**
     * Called when an entity is killed but we only have position
     */
    private void onEntityKilledAtPosition(double x, double y, double z) {
        // Kill Sound
        if (killSoundEnabled.isEnabled()) {
            playSound(killSound.getSelected(), KILL_SOUNDS, KILL_SOUND_FILES, (float) killSoundVolume.getValue() / 100f);
        }

        // Kill Effect
        if (killEffectEnabled.isEnabled()) {
            spawnKillEffect(x, y, z);
        }
    }

    // ==================== SOUND PLAYBACK ====================

    // Sounds with multiple variants (random selection)
    private static final java.util.Random soundRandom = new java.util.Random();

    /**
     * Play sound immediately - creates new clip each time to avoid cooldown issues
     * Supports sounds with multiple variants (e.g., boykisser-1 to boykisser-6)
     */
    private void playSound(String soundName, String[] names, String[] files, float volume) {
        new Thread(() -> {
            try {
                int soundIndex = getSoundIndex(soundName, names);
                String soundFile = files[soundIndex];

                // Handle sounds with variants
                soundFile = getVariantSoundFile(soundFile);

                InputStream is = getClass().getResourceAsStream(soundFile);
                if (is == null) {
                    is = Effects.class.getResourceAsStream(soundFile);
                }
                if (is == null) {
                    System.out.println("[Effects] Sound not found: " + soundFile);
                    return;
                }

                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bis);

                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                // Set volume
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
            } catch (Exception e) {
                // Silent fail
            }
        }).start();
    }

    /**
     * Get a random variant for sounds that have multiple files
     */
    private String getVariantSoundFile(String basePath) {
        // Check for sounds with variants
        if (basePath.contains("boykisser")) {
            int variant = soundRandom.nextInt(6) + 1; // 1-6
            return basePath.replace("boykisser.wav", "boykisser-" + variant + ".wav");
        } else if (basePath.contains("click.wav")) {
            int variant = soundRandom.nextInt(3) + 1; // 1-3
            return basePath.replace("click.wav", "click-" + variant + ".wav");
        } else if (basePath.contains("glass.wav")) {
            int variant = soundRandom.nextInt(3) + 1; // 1-3
            return basePath.replace("glass.wav", "glass-" + variant + ".wav");
        } else if (basePath.contains("moan.wav")) {
            int variant = soundRandom.nextInt(4) + 1; // 1-4
            return basePath.replace("moan.wav", "moan-" + variant + ".wav");
        }
        return basePath;
    }

    // ==================== HIT EFFECT ====================

    private void spawnHitEffect(EntityLivingBase entity) {
        if (mc.theWorld == null) return;

        String effect = hitEffect.getSelected();
        int intensity = (int) hitEffectIntensity.getValue();

        double x = entity.posX;
        double z = entity.posZ;

        // Spawn at 3 heights: head, chest, legs
        double headY = entity.posY + entity.height - 0.2;
        double chestY = entity.posY + entity.height * 0.6;
        double legsY = entity.posY + entity.height * 0.2;

        double[] heights = {headY, chestY, legsY};

        switch (effect) {
            case "Blood":
                // Spawn block crack particles using redstone block at all body parts
                int blockId = Block.getIdFromBlock(Blocks.redstone_block);
                for (double y : heights) {
                    for (int i = 0; i < intensity / 3 + 1; i++) {
                        double offsetX = (Math.random() - 0.5) * 0.8;
                        double offsetY = (Math.random() - 0.5) * 0.3;
                        double offsetZ = (Math.random() - 0.5) * 0.8;
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.BLOCK_CRACK,
                            x + offsetX, y + offsetY, z + offsetZ,
                            (Math.random() - 0.5) * 0.5,
                            Math.random() * 0.3,
                            (Math.random() - 0.5) * 0.5,
                            blockId
                        );
                    }
                }
                break;

            case "Fire":
                // Fire and lava particles - LiquidBounce style
                for (double y : heights) {
                    for (int i = 0; i < intensity / 3 + 1; i++) {
                        double offsetX = (Math.random() - 0.5) * 0.8;
                        double offsetY = (Math.random() - 0.5) * 0.3;
                        double offsetZ = (Math.random() - 0.5) * 0.8;
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.FLAME,
                            x + offsetX, y + offsetY, z + offsetZ,
                            (Math.random() - 0.5) * 0.2,
                            Math.random() * 0.15,
                            (Math.random() - 0.5) * 0.2
                        );
                        if (i % 2 == 0) {
                            mc.theWorld.spawnParticle(
                                EnumParticleTypes.LAVA,
                                x + offsetX, y + offsetY, z + offsetZ,
                                0, 0, 0
                            );
                        }
                    }
                }
                break;

            case "Heart":
                // Heart particles - LiquidBounce style
                for (double y : heights) {
                    for (int i = 0; i < intensity / 4 + 1; i++) {
                        double offsetX = (Math.random() - 0.5) * 0.8;
                        double offsetY = (Math.random() - 0.5) * 0.3;
                        double offsetZ = (Math.random() - 0.5) * 0.8;
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.HEART,
                            x + offsetX, y + offsetY + 0.3, z + offsetZ,
                            (Math.random() - 0.5) * 0.1,
                            Math.random() * 0.1,
                            (Math.random() - 0.5) * 0.1
                        );
                    }
                }
                break;

            case "Water":
                // Water splash and drip particles - LiquidBounce style
                for (double y : heights) {
                    for (int i = 0; i < intensity / 3 + 1; i++) {
                        double offsetX = (Math.random() - 0.5) * 0.8;
                        double offsetY = (Math.random() - 0.5) * 0.3;
                        double offsetZ = (Math.random() - 0.5) * 0.8;
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.WATER_SPLASH,
                            x + offsetX, y + offsetY, z + offsetZ,
                            (Math.random() - 0.5) * 0.3,
                            Math.random() * 0.2,
                            (Math.random() - 0.5) * 0.3
                        );
                        if (i % 2 == 0) {
                            mc.theWorld.spawnParticle(
                                EnumParticleTypes.DRIP_WATER,
                                x + offsetX, y + offsetY + 0.2, z + offsetZ,
                                0, 0, 0
                            );
                        }
                    }
                }
                break;

            case "Smoke":
                // Smoke particles - LiquidBounce style
                for (double y : heights) {
                    for (int i = 0; i < intensity / 3 + 1; i++) {
                        double offsetX = (Math.random() - 0.5) * 0.8;
                        double offsetY = (Math.random() - 0.5) * 0.3;
                        double offsetZ = (Math.random() - 0.5) * 0.8;
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.SMOKE_LARGE,
                            x + offsetX, y + offsetY, z + offsetZ,
                            (Math.random() - 0.5) * 0.15,
                            Math.random() * 0.1,
                            (Math.random() - 0.5) * 0.15
                        );
                    }
                }
                break;

            case "Magic":
                // Enchant and witch particles - LiquidBounce style
                for (double y : heights) {
                    for (int i = 0; i < intensity / 3 + 1; i++) {
                        double offsetX = (Math.random() - 0.5) * 0.8;
                        double offsetY = (Math.random() - 0.5) * 0.3;
                        double offsetZ = (Math.random() - 0.5) * 0.8;
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.ENCHANTMENT_TABLE,
                            x + offsetX, y + offsetY, z + offsetZ,
                            (Math.random() - 0.5) * 0.2,
                            Math.random() * 0.1,
                            (Math.random() - 0.5) * 0.2
                        );
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.SPELL_WITCH,
                            x + offsetX, y + offsetY, z + offsetZ,
                            (Math.random() - 0.5) * 0.1,
                            Math.random() * 0.05,
                            (Math.random() - 0.5) * 0.1
                        );
                    }
                }
                break;

            case "Crits":
                // Crit and magic crit particles - LiquidBounce style
                for (double y : heights) {
                    for (int i = 0; i < intensity / 3 + 1; i++) {
                        double offsetX = (Math.random() - 0.5) * 0.8;
                        double offsetY = (Math.random() - 0.5) * 0.3;
                        double offsetZ = (Math.random() - 0.5) * 0.8;
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.CRIT,
                            x + offsetX, y + offsetY, z + offsetZ,
                            (Math.random() - 0.5) * 0.3,
                            Math.random() * 0.2,
                            (Math.random() - 0.5) * 0.3
                        );
                        mc.theWorld.spawnParticle(
                            EnumParticleTypes.CRIT_MAGIC,
                            x + offsetX, y + offsetY, z + offsetZ,
                            (Math.random() - 0.5) * 0.3,
                            Math.random() * 0.2,
                            (Math.random() - 0.5) * 0.3
                        );
                    }
                }
                break;
        }
    }

    // ==================== KILL EFFECT ====================

    private void spawnKillEffect(double x, double y, double z) {
        if (mc.theWorld == null) return;

        String effect = killEffect.getSelected();

        switch (effect) {
            case "Lightning":
                // Spawn lightning bolt (client-side only)
                EntityLightningBolt lightning = new EntityLightningBolt(mc.theWorld, x, y, z);
                mc.theWorld.addWeatherEffect(lightning);
                break;

            case "Explosion":
                // Spawn explosion particles
                mc.theWorld.spawnParticle(
                    EnumParticleTypes.EXPLOSION_HUGE,
                    x, y + 1, z,
                    0, 0, 0
                );
                // Also spawn some smoke
                for (int i = 0; i < 20; i++) {
                    double offsetX = (Math.random() - 0.5) * 2;
                    double offsetY = Math.random() * 2;
                    double offsetZ = (Math.random() - 0.5) * 2;
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.SMOKE_LARGE,
                        x + offsetX, y + offsetY, z + offsetZ,
                        0, 0.1, 0
                    );
                }
                break;

            case "Totem":
                // Spawn totem particles (golden sparkles)
                for (int i = 0; i < 50; i++) {
                    double offsetX = (Math.random() - 0.5) * 2;
                    double offsetY = Math.random() * 2;
                    double offsetZ = (Math.random() - 0.5) * 2;
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.FLAME,
                        x + offsetX, y + offsetY, z + offsetZ,
                        (Math.random() - 0.5) * 0.1,
                        Math.random() * 0.1,
                        (Math.random() - 0.5) * 0.1
                    );
                }
                break;
        }
    }

    // ==================== UTILITY ====================

    private int getSoundIndex(String soundName, String[] sounds) {
        for (int i = 0; i < sounds.length; i++) {
            if (sounds[i].equals(soundName)) {
                return i;
            }
        }
        return 0;
    }

    public static Effects getInstance() {
        return instance;
    }
}
