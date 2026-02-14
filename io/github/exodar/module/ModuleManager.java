/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module;

import io.github.exodar.event.EventBus;
import io.github.exodar.module.modules.combat.AutoBlock;
import io.github.exodar.module.modules.combat.AutoClicker;
import io.github.exodar.module.modules.combat.HitSelect;
// import io.github.exodar.module.modules.combat.AutoGHead;
import io.github.exodar.module.modules.combat.AutoSoup;
import io.github.exodar.module.modules.combat.Backtrack;
import io.github.exodar.module.modules.combat.BlockHit;
import io.github.exodar.module.modules.combat.BowAimbot;
import io.github.exodar.module.modules.combat.NoHitDelay;
// import io.github.exodar.module.modules.combat.Penetration; // OLD - replaced by Penetration2
import io.github.exodar.module.modules.combat.Penetration2;
import io.github.exodar.module.modules.combat.Reach;
// import io.github.exodar.module.modules.combat.TimerRange;
// import io.github.exodar.module.modules.combat.TimerRangeV2;
import io.github.exodar.module.modules.combat.AimAssist;
import io.github.exodar.module.modules.combat.BowAssist;
import io.github.exodar.module.modules.combat.Velocity;
import io.github.exodar.module.modules.combat.NoVelocity;
// import io.github.exodar.module.modules.combat.Velocity2; // Disabled - not working well
import io.github.exodar.module.modules.combat.LagRange;
import io.github.exodar.module.modules.combat.AutoPotion;
// import io.github.exodar.module.modules.combat.Criticals;
import io.github.exodar.module.modules.combat.TeleportHit;
import io.github.exodar.module.modules.combat.DashHit;
// import io.github.exodar.module.modules.combat.TimerRange69;
import io.github.exodar.module.modules.combat.TimerRangeTest;
import io.github.exodar.module.modules.combat.TimerRangeV4;
import io.github.exodar.module.modules.player.FakeLag;
import io.github.exodar.module.modules.player.FastPlace;
import io.github.exodar.module.modules.player.RightClicker;
import io.github.exodar.module.modules.player.FastBreak;
// Old Scaffold files removed - using new Scaffold in movement package
import io.github.exodar.module.modules.player.GameSpeed;
import io.github.exodar.module.modules.player.AirStuck;
import io.github.exodar.module.modules.player.AutoTool;
import io.github.exodar.module.modules.player.Refill;
import io.github.exodar.module.modules.player.ChestStealer;
import io.github.exodar.module.modules.player.AutoArmor;
import io.github.exodar.module.modules.player.InvCleaner;
import io.github.exodar.module.modules.player.AutoSort;
import io.github.exodar.module.modules.player.AutoSword;
import io.github.exodar.module.modules.player.Blink;
import io.github.exodar.module.modules.player.NoItemRelease;
import io.github.exodar.module.modules.player.Ping;
import io.github.exodar.module.modules.player.PingV2;
import io.github.exodar.module.modules.player.PingV3;
import io.github.exodar.module.modules.player.TestInvManager;
import io.github.exodar.module.modules.movement.NoFall;
import io.github.exodar.module.modules.movement.Sprint;
import io.github.exodar.module.modules.movement.NoJumpDelay;
import io.github.exodar.module.modules.movement.InvMove;
import io.github.exodar.module.modules.movement.Flight;
import io.github.exodar.module.modules.movement.Speed;
import io.github.exodar.module.modules.movement.Scaffold2;
import io.github.exodar.module.modules.movement.Step;
import io.github.exodar.module.modules.movement.SaveMoveKeys;
import io.github.exodar.module.modules.movement.KeepSprint;
import io.github.exodar.module.modules.movement.NoSlow;
import io.github.exodar.module.modules.movement.Clutch;
import io.github.exodar.module.modules.movement.Spider;
import io.github.exodar.module.modules.movement.Phase;
import io.github.exodar.module.modules.movement.Scaffold;
import io.github.exodar.module.modules.world.SafeWalk;
import io.github.exodar.module.modules.world.AutoPlace;
// import io.github.exodar.module.modules.player.AutoTool;
import io.github.exodar.module.modules.world.BridgeAssist;
import io.github.exodar.module.modules.render.NoHurtCam;
import io.github.exodar.module.modules.render.Nametags;
import io.github.exodar.module.modules.render.Hitbox;
import io.github.exodar.module.modules.render.Fullbright;
import io.github.exodar.module.modules.render.ItemTest;
import io.github.exodar.module.modules.render.ESP;
import io.github.exodar.module.modules.render.Skeleton;
import io.github.exodar.module.modules.render.Chams;
// import io.github.exodar.module.modules.render.Glow; // DISABLED - performance optimization
import io.github.exodar.module.modules.render.BedESP;
import io.github.exodar.module.modules.render.FreeLook;
import io.github.exodar.module.modules.render.ItemESP;
import io.github.exodar.module.modules.render.Trajectories;
import io.github.exodar.module.modules.render.Indicators;
import io.github.exodar.module.modules.render.Tracers;
import io.github.exodar.module.modules.render.Pointers;
import io.github.exodar.module.modules.render.Radar;
import io.github.exodar.module.modules.render.Xray;
import io.github.exodar.module.modules.render.ViewClip;
import io.github.exodar.module.modules.render.ChestESP;
import io.github.exodar.module.modules.render.BarrierESP;
import io.github.exodar.module.modules.render.AntiDebuff;
import io.github.exodar.module.modules.render.Animations;
import io.github.exodar.module.modules.player.BlockIn;
import io.github.exodar.module.modules.combat.WTap;
import io.github.exodar.module.modules.combat.AntiFireball;
// import io.github.exodar.module.modules.combat.MoreKB; // Needs more precision (1ms steps)
import io.github.exodar.module.modules.combat.STap;
// import io.github.exodar.module.modules.combat.LegitAura;
// import io.github.exodar.module.modules.combat.LegitAura3;
// import io.github.exodar.module.modules.combat.LegitAura6;
import io.github.exodar.module.modules.combat.SilentAura;
import io.github.exodar.module.modules.misc.Hud;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.module.modules.misc.AntiBot;
// import io.github.exodar.module.modules.misc.Friends; // Moved to Settings
import io.github.exodar.module.modules.misc.AutoRegister;
import io.github.exodar.module.modules.misc.AdventureMode;
import io.github.exodar.module.modules.misc.NoVanillaNametags;
import io.github.exodar.module.modules.misc.CPSSpoof;
import io.github.exodar.module.modules.misc.Disabler;
import io.github.exodar.module.modules.misc.AntiAFK;
import io.github.exodar.module.modules.misc.InventoryTracker;
import io.github.exodar.module.modules.misc.BedrockSpoof;
import io.github.exodar.module.modules.misc.AntiObfuscate;
import io.github.exodar.module.modules.misc.AntiAim;
import io.github.exodar.module.modules.misc.BelowNameDebug;
import io.github.exodar.module.modules.misc.TestFont;
import io.github.exodar.module.modules.misc.Testing;
import io.github.exodar.module.modules.misc.HackerDetector;
import io.github.exodar.module.modules.misc.Effects;
import io.github.exodar.module.modules.misc.Parkour;
import io.github.exodar.module.modules.misc.SumoFences;
import io.github.exodar.module.modules.misc.MurderMystery;
import io.github.exodar.module.modules.misc.Spoof;
import io.github.exodar.module.modules.misc.PacketLogger;
import io.github.exodar.module.modules.misc.AntiCrash;
import io.github.exodar.module.modules.misc.KillSults;
// import io.github.exodar.module.modules.misc.LunarUnlock; // Disabled - requires Java Agent for bytecode patching
// import io.github.exodar.module.modules.misc.HitSound; // Replaced by Effects module
import io.github.exodar.module.modules.player.BedNuker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {
    private List<Module> modules;
    private Map<String, Module> modulesByName;  // Fast O(1) lookup cache

    public ModuleManager() {
        modules = new ArrayList<>();
        modulesByName = new HashMap<>();

        // System.out.println("[ModuleManager] Initializing modules...");

        try {
            // Combat modules
            // System.out.println("[ModuleManager] Loading AutoClicker...");
            modules.add(new AutoClicker());
            modules.add(new HitSelect());
            // System.out.println("[ModuleManager] Loading AutoBlock...");
            modules.add(new AutoBlock());
            // modules.add(new AutoGHead());
            // modules.add(new AutoSoup()); // TODO: Fix
            // modules.add(new BlockHit());
            // System.out.println("[ModuleManager] Loading AimAssist...");
            modules.add(new AimAssist());
            modules.add(new BowAssist());
            // modules.add(new SilentAura()); // DISABLED - not working correctly
            // DISABLED - LegitAura modules
            // modules.add(new LegitAura());
            // modules.add(new LegitAura3());
            // modules.add(new LegitAura6());
            // System.out.println("[ModuleManager] Loading Backtrack...");
            modules.add(new Backtrack());
            // System.out.println("[ModuleManager] Loading No Hit Delay...");
            modules.add(new NoHitDelay());
            // TEMPORARILY DISABLED - TimerRange modules not working correctly
            // System.out.println("[ModuleManager] Loading TimerRange...");
            // modules.add(new TimerRange());
            // System.out.println("[ModuleManager] Loading TimerRangeV2 (Augustus)...");
            // modules.add(new TimerRangeV2());
            // System.out.println("[ModuleManager] Loading Velocity...");
            modules.add(new Velocity());
            modules.add(new NoVelocity());
            // modules.add(new Velocity2()); // Disabled - not working well
            // System.out.println("[ModuleManager] Loading Penetration...");
            modules.add(new Penetration2()); // New simple Penetration - works with Reach
            // System.out.println("[ModuleManager] Loading Reach...");
            modules.add(new Reach());
            // DISABLED - BowAimbot
            // modules.add(new BowAimbot());
            // System.out.println("[ModuleManager] Loading LagRange...");
            modules.add(new LagRange());
            // System.out.println("[ModuleManager] Loading AutoPotion...");
            modules.add(new AutoPotion());
            // System.out.println("[ModuleManager] Loading Criticals...");
            // modules.add(new Criticals());
            // System.out.println("[ModuleManager] Loading TeleportHit...");
            modules.add(new TeleportHit());
            modules.add(new DashHit());
            // System.out.println("[ModuleManager] Loading Wtap...");
            modules.add(new WTap());
            // modules.add(new MoreKB()); // Needs more precision (1ms steps)
            modules.add(new STap());
            // System.out.println("[ModuleManager] Loading AntiFireball...");
            modules.add(new AntiFireball());
            // System.out.println("[ModuleManager] Loading TimerRange69 (Gothaj)...");
            // modules.add(new TimerRange69());
            // DISABLED - TimerRange modules
            // System.out.println("[ModuleManager] Loading TimerRangeTest...");
            // modules.add(new TimerRangeTest());
            // System.out.println("[ModuleManager] Loading TimerRangeV4...");
            // modules.add(new TimerRangeV4());

            // Player modules
            // System.out.println("[ModuleManager] Loading FastPlace...");
            modules.add(new FastPlace());
            // System.out.println("[ModuleManager] Loading FastBreak...");
            modules.add(new FastBreak());
            // System.out.println("[ModuleManager] Loading Scaffold...");
            // Old player.Scaffold files removed - using new movement.Scaffold
            // System.out.println("[ModuleManager] Loading GameSpeed...");
            modules.add(new GameSpeed());
            modules.add(new AirStuck());
            // System.out.println("[ModuleManager] Loading Auto Tool...");
            modules.add(new AutoTool());
            // System.out.println("[ModuleManager] Loading Refill...");
            modules.add(new Refill());
            // System.out.println("[ModuleManager] Loading ChestStealer...");
            modules.add(new ChestStealer());
            // System.out.println("[ModuleManager] Loading AutoArmor...");
            modules.add(new AutoArmor());
            // System.out.println("[ModuleManager] Loading InvCleaner...");
            modules.add(new InvCleaner());
            modules.add(new AutoSort());
            // modules.add(new TestInvManager()); // Disabled - using separate modules (AutoArmor, InvCleaner, AutoSort)
            // System.out.println("[ModuleManager] Loading AutoSword...");
            // modules.add(new AutoSword()); // Disabled - using AutoSort instead
            // System.out.println("[ModuleManager] Loading Blink...");
            modules.add(new Blink());
            // modules.add(new BedNuker());
            // System.out.println("[ModuleManager] Loading NoItemRelease...");
            modules.add(new NoItemRelease());
            // System.out.println("[ModuleManager] Loading Ping...");
            modules.add(new Ping());
            modules.add(new PingV2());
            modules.add(new PingV3());
            // System.out.println("[ModuleManager] Loading FakeLag...");
            modules.add(new FakeLag());
            // System.out.println("[ModuleManager] Loading AntiDebuff...");
            modules.add(new AntiDebuff());
            // System.out.println("[ModuleManager] Loading BlockIn...");
            modules.add(new BlockIn());
            // DISABLED - RightClicker
            // System.out.println("[ModuleManager] Loading RightClicker...");
            // modules.add(new RightClicker());

            // Movement modules
            // DISABLED - NoFall
            // modules.add(new NoFall());
            // System.out.println("[ModuleManager] Loading Sprint...");
            modules.add(new Sprint());
            // modules.add(new NoSlow());
            // System.out.println("[ModuleManager] Loading No Jump Delay...");
            modules.add(new NoJumpDelay());
            // System.out.println("[ModuleManager] Loading Inv Move...");
            modules.add(new InvMove());
            // System.out.println("[ModuleManager] Loading Flight...");
            modules.add(new Flight());
            // System.out.println("[ModuleManager] Loading Speed...");
            modules.add(new Speed());
            // New movement modules
            modules.add(new Step());
            // System.out.println("[ModuleManager] Loading SaveMoveKeys...");
            modules.add(new SaveMoveKeys());
            modules.add(new Clutch());
            modules.add(new Spider());
            modules.add(new Phase());
            modules.add(new Scaffold());  // New unified Scaffold with Blatant/Legit modes
            // modules.add(new Scaffold2()); // Disabled - old version
            // DISABLED - KeepSprint
            // modules.add(new KeepSprint());

            // World/Player modules
            // SafeWalk - NOW WORKS with SafeWalkEvent hook!
            System.out.println("[ModuleManager] Loading SafeWalk...");
            modules.add(new SafeWalk());
            // DISABLED - AutoPlace
            // System.out.println("[ModuleManager] Loading AutoPlace...");
            // modules.add(new AutoPlace());
            // DISABLED - BridgeAssist
            // modules.add(new BridgeAssist());

            // Render modules
            // DISABLED - NoHurtCam
            // modules.add(new NoHurtCam());
            // System.out.println("[ModuleManager] Loading Nametags...");
            modules.add(new Nametags());
            // System.out.println("[ModuleManager] Loading Hitbox...");
            modules.add(new Hitbox());
            modules.add(new Fullbright());
            modules.add(new Animations());
            // DISABLED - ItemTest
            // modules.add(new ItemTest());

            // Misc modules
            // System.out.println("[ModuleManager] Loading Teams...");
            modules.add(new Teams());
            modules.add(new AntiBot());
            modules.add(new BedrockSpoof());
            // Friends removed - now in Settings panel
            // System.out.println("[ModuleManager] Loading HUD...");
            modules.add(new Hud());
            // System.out.println("[ModuleManager] Loading AutoRegister...");
            modules.add(new AutoRegister());
            // Disabler - Desactivado (no funciona bien)
            // modules.add(new Disabler());
            modules.add(new AntiAFK());
            modules.add(new InventoryTracker());
            modules.add(new AntiObfuscate());
            modules.add(new AntiAim());
            modules.add(new BelowNameDebug());
            modules.add(new TestFont());
            modules.add(new Testing());
            modules.add(new HackerDetector());
            modules.add(new Effects());
            modules.add(new Parkour());
            // modules.add(new SumoFences()); // TODO: Fix
            modules.add(new MurderMystery());
            modules.add(new Spoof());
            modules.add(new PacketLogger());
            modules.add(new AntiCrash());
            modules.add(new KillSults());
            // modules.add(new LunarUnlock()); // Disabled - requires Java Agent for bytecode patching
            // modules.add(new HitSound()); // Replaced by Effects module
            // DISABLED - AdventureMode
            // modules.add(new AdventureMode());
            // DISABLED - NoVanillaNametags
            // modules.add(new NoVanillaNametags());
            // DISABLED - CPSSpoof
            // modules.add(new CPSSpoof());
            // DISABLED - NametagsRaven (incompatible con mappings de Lunar Client)
            // System.out.println("[ModuleManager] Loading NametagsRaven...");
            // modules.add(new NametagsRaven());

            // Visual modules
            // System.out.println("[ModuleManager] Loading ESP...");
            modules.add(new ESP());
            // System.out.println("[ModuleManager] Loading Skeleton...");
            modules.add(new Skeleton());
            // System.out.println("[ModuleManager] Loading Chams...");
            modules.add(new Chams());
            // DISABLED - Glow
            // modules.add(new Glow());
            // System.out.println("[ModuleManager] Loading BedESP...");
            modules.add(new BedESP());
            // System.out.println("[ModuleManager] Loading ItemESP...");
            modules.add(new ItemESP());
            // System.out.println("[ModuleManager] Loading Trajectories...");
            modules.add(new Trajectories());
            modules.add(new Indicators());
            // System.out.println("[ModuleManager] Loading Tracers...");
            modules.add(new Tracers());
            // System.out.println("[ModuleManager] Loading Pointers...");
            modules.add(new Pointers());
            // DISABLED - Radar
            // modules.add(new Radar());
            // System.out.println("[ModuleManager] Loading Xray...");
            modules.add(new Xray());
            modules.add(new ChestESP());
            modules.add(new BarrierESP());
            // DISABLED - ViewClip
            // modules.add(new ViewClip());
            modules.add(new FreeLook());

            // Nametags module for Lunar Client 1.8.9
            // TEMPORARILY DISABLED - Nametags (for debugging)
            // System.out.println("[ModuleManager] Loading Nametags...");
            // modules.add(new Nametags());
            // System.out.println("[ModuleManager] Loading NametagsV1...");
            // modules.add(new NametagsV1());
            // System.out.println("[ModuleManager] Loading NametagsV2...");
            // modules.add(new NametagsV2());
            // System.out.println("[ModuleManager] Loading NametagsV3...");
            // modules.add(new NametagsV3());
            // System.out.println("[ModuleManager] Loading NametagsV4...");
            // modules.add(new NametagsV4());
            // System.out.println("[ModuleManager] Loading NametagsV5...");
            // modules.add(new NametagsV5());
            // System.out.println("[ModuleManager] Loading NametagsV6...");
            // modules.add(new NametagsV6());

            // Debug modules
            // TEMPORARILY DISABLED - PlayerDetectionTest (for debugging)
            // System.out.println("[ModuleManager] Loading PlayerDetectionTest...");
            // modules.add(new PlayerDetectionTest());
        } catch (Throwable t) {
            // System.out.println("[ModuleManager] ERROR loading module: " + t.getMessage());
            t.printStackTrace();
            // System.out.println("[ModuleManager]   Error type: " + t.getClass().getName());
            // DO NOT RETHROW - let constructor complete
        }

        // System.out.println("[ModuleManager] Initialized " + modules.size() + " modules:");
        // for (Module m : modules) {
        //     System.out.println("[ModuleManager]   - " + m.getName() + " (" + m.getCategory() + ")");
        // }

        // Populate fast lookup HashMap
        for (Module module : modules) {
            modulesByName.put(module.getName().toLowerCase(), module);
        }

        for (Module module : getModules()) {
            EventBus.register(module);
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModuleByName(String name) {
        // Fast O(1) lookup using HashMap
        if (name == null) return null;
        return modulesByName.get(name.toLowerCase());
    }

    public void onUpdate() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onUpdate();
            }
        }
    }

    /**
     * Disable all enabled modules - called on unload
     */
    public void disableAll() {
        // System.out.println("[ModuleManager] Disabling all modules...");
        int disabledCount = 0;

        for (Module module : modules) {
            if (module.isEnabled()) {
                try {
                    // System.out.println("[ModuleManager] Disabling: " + module.getName());
                    // setEnabled(false) will automatically call onDisable()
                    module.setEnabled(false);
                    disabledCount++;
                } catch (Exception e) {
                    // System.out.println("[ModuleManager] Error disabling " + module.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // System.out.println("[ModuleManager] Disabled " + disabledCount + " modules");
    }

    /**
     * Complete cleanup - called on unload
     */
    public void cleanup() {
        disableAll();
        // System.out.println("[ModuleManager] Cleanup complete");
    }
}
