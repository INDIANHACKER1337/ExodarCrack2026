/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
///// LOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOLLL
/////
/////
package io.github.exodar;

import io.github.exodar.event.EventBus;
import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.TickStartEvent;
import io.github.exodar.event.TickEndEvent;
import io.github.exodar.event.PreMotionEvent;
import io.github.exodar.event.PostMotionEvent;
import io.github.exodar.event.MoveInputEvent;
import io.github.exodar.event.BlockShapeEvent;
import io.github.exodar.event.PlayerJumpEvent;
import io.github.exodar.event.SafeWalkEvent;
import io.github.exodar.event.AttackEvent;
import io.github.exodar.event.auxiliary.MoveInputEventAux;
import io.github.exodar.event.auxiliary.PreUpdateEventAux;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import io.github.exodar.hook.GuiRenderHook;
import io.github.exodar.hook.KeyboardHook;
import io.github.exodar.listener.RenderListener;
import io.github.exodar.patcher.RenderEventBridge;
import io.github.exodar.patcher.AttackEventBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import io.github.exodar.command.CommandManager;
import io.github.exodar.internal.EventHandler;
import io.github.exodar.internal.Thrower;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.render.Nametags;
import io.github.exodar.module.modules.render.ItemTest;
import io.github.exodar.module.modules.player.Blink;
import io.github.exodar.module.modules.combat.Backtrack;
import io.github.exodar.module.modules.combat.TimerRange;
import io.github.exodar.module.modules.combat.STap;
import io.github.exodar.module.modules.combat.MoreKB;
import io.github.exodar.module.modules.combat.Criticals;
import io.github.exodar.module.modules.combat.LagRange;
import io.github.exodar.module.modules.misc.BedrockSpoof;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.NetworkManager;
import net.minecraft.entity.player.EntityPlayer;
import io.netty.channel.ChannelHandlerContext;
import io.github.exodar.gui.SkeetClickGui;
import io.github.exodar.config.ConfigManager;
import io.github.exodar.utils.FontRendererHelper;
import io.github.exodar.api.BuildInfo;
import io.github.exodar.api.ExodarAPI;
import io.github.exodar.render.RendererInstaller;
import io.github.exodar.ui.HudRenderThread;
import io.github.exodar.hook.EntityRenderHook;
import io.github.exodar.hook.ItemRendererHook;
import io.github.exodar.render.Render3DManager;
import io.github.exodar.websocket.ExodarWebSocketServer;
import org.lwjgl.input.Keyboard;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static io.github.exodar.internal.patcher.MethodModifier.Type.ON_RETURN_THROW;
import static io.github.exodar.internal.patcher.MethodModifier.Type.ON_ENTRY;
import io.github.exodar.internal.Canceler;

public class Main
{
    private static Field playerField = null;
    private static Method addChatMethod = null;
    private static ModuleManager moduleManager = null;
    private static ConfigManager configManager = null;
    private static ExodarWebSocketServer webSocketServer = null;
    private static final int WEBSOCKET_PORT = 8080;
    private static Field thePlayerField = null;
    private static Field theWorldField = null;
    private static ScheduledExecutorService executor = null;

    // ArrayList optimization - cache enabled modules
    private static java.util.List<io.github.exodar.module.Module> cachedEnabledModules = new java.util.ArrayList<>();
    private static int lastModuleStateHash = 0;

    // Session tracking for API heartbeat
    private static String lastServerIp = null;
    private static boolean wasConnectedToServer = false;

    // Middle-click friend toggle state
    private static boolean wasMiddleMousePressed = false;

    // NOTE: worldRenderPending flag removed - ESP modules now use Render3DEvent
    // from EntityRenderHook proxy for correct GL matrices

    private static EntityPlayerSP getPlayer(Minecraft mc) {
        try {
            if (playerField == null) {
                // Find the EntityPlayerSP field by type, not by name
                // System.out.println("[Exodar] Searching for player field...");
                for (Field field : mc.getClass().getDeclaredFields()) {
                    // System.out.println("[Exodar] Field: " + field.getName() + " Type: " + field.getType().getName());
                    if (field.getType().getName().contains("EntityPlayerSP")) {
                        playerField = field;
                        playerField.setAccessible(true);
                        // System.out.println("[Exodar] Found player field: " + field.getName());
                        break;
                    }
                }
            }
            if (playerField != null) {
                return (EntityPlayerSP) playerField.get(mc);
            }
        } catch (Exception e) {
            // System.out.println("[Exodar] Error getting player: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static void sendChatMessage(EntityPlayerSP player, String message) {
        try {
            if (addChatMethod == null) {
                // Try to find addChatMessage method
                for (Method method : player.getClass().getMethods()) {
                    if (method.getName().contains("addChat") || method.getName().contains("sendChat")) {
                        if (method.getParameterCount() == 1) {
                            addChatMethod = method;
                            // System.out.println("[Exodar] Found chat method: " + method.getName());
                            break;
                        }
                    }
                }
            }

            if (addChatMethod != null) {
                addChatMethod.invoke(player, new ChatComponentText(message));
            } else {
                // Fallback: try direct method
                player.addChatMessage(new ChatComponentText(message));
            }
        } catch (Exception e) {
            // System.out.println("[Exodar] Error sending chat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Cached network manager and method for sending packets
    private static Object cachedNetworkManager = null;
    private static Method cachedSendPacketMethod = null;
    private static boolean networkReflectionInitialized = false;

    /**
     * Send a chat message as the player (to the server)
     * This is for commands from the admin panel to make the player execute commands
     */
    /**
     * Send chat message or execute command
     * - If message starts with /, executes as command
     * - Otherwise sends as regular chat message
     * Same behavior as AutoRegister
     */
    private static void sendPlayerChatMessage(EntityPlayerSP player, String message) {
        try {
            if (player != null && message != null && !message.isEmpty()) {
                // sendChatMessage handles both:
                // - Commands: /command args (starts with /)
                // - Chat: regular text (doesn't start with /)
                player.sendChatMessage(message);
            }
        } catch (Exception e) {
            // Fallback: try via packet (for chat only, not commands)
            try {
                if (!networkReflectionInitialized) {
                    initNetworkReflection(player);
                    networkReflectionInitialized = true;
                }
                if (cachedNetworkManager != null && cachedSendPacketMethod != null) {
                    C01PacketChatMessage chatPacket = new C01PacketChatMessage(message);
                    cachedSendPacketMethod.invoke(cachedNetworkManager, chatPacket);
                }
            } catch (Exception ex) {
                // Silent fail
            }
        }
    }

    private static void initNetworkReflection(EntityPlayerSP player) {
        try {
            // Find NetHandler from player
            Object netHandler = null;
            for (Field f : player.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType().getName().contains("NetHandler")) {
                    netHandler = f.get(player);
                    break;
                }
            }
            if (netHandler == null) {
                for (Field f : player.getClass().getSuperclass().getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.getType().getName().contains("NetHandler")) {
                        netHandler = f.get(player);
                        break;
                    }
                }
            }

            if (netHandler != null) {
                // System.out.println("[Exodar] Found NetHandler: " + netHandler.getClass().getName());
                // Find NetworkManager in NetHandler
                for (Field f : netHandler.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.getType().getName().contains("NetworkManager")) {
                        cachedNetworkManager = f.get(netHandler);
                        // System.out.println("[Exodar] Found NetworkManager");
                        break;
                    }
                }
                // Find sendPacket method
                if (cachedNetworkManager != null) {
                    for (Method m : cachedNetworkManager.getClass().getMethods()) {
                        if (m.getName().contains("sendPacket") && m.getParameterCount() == 1) {
                            cachedSendPacketMethod = m;
                            // System.out.println("[Exodar] Found sendPacket method: " + m.getName());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // System.out.println("[Exodar] Error initializing network reflection: " + e.getMessage());
        }
    }

    /**
     * Check if a key or mouse button is pressed
     * @param keyCode Positive = keyboard key, Negative = mouse button (-100 to -104)
     * @return true if pressed
     */
    private static boolean isKeyOrMousePressed(int keyCode) {
        if (keyCode < 0) {
            // Mouse button (-100 = button 0, -101 = button 1, etc.)
            int mouseButton = keyCode + 100;
            return org.lwjgl.input.Mouse.isButtonDown(mouseButton);
        } else {
            // Keyboard key
            return Keyboard.isKeyDown(keyCode);
        }
    }

    private static void initMinecraftFields() {
        if (thePlayerField == null) {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                // Buscar thePlayer
                for (Field f : mc.getClass().getDeclaredFields()) {
                    if (f.getType().getName().contains("EntityPlayer")) {
                        f.setAccessible(true);
                        thePlayerField = f;
                        break;
                    }
                }

                // Buscar theWorld
                for (Field f : mc.getClass().getDeclaredFields()) {
                    if (f.getType().getName().contains("World")) {
                        f.setAccessible(true);
                        theWorldField = f;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // warning called from c++ thread
    public static void onLoad()
    {
        // System.out.println("[Exodar] Successfully injected into Minecraft 1.8.9!");
        // System.out.println("[Exodar] Welcome to Exodar Client!");

        // Inicializar campos de Minecraft
        initMinecraftFields();

        // Inicializar el executor para tareas programadas
        executor = Executors.newScheduledThreadPool(1);
        // System.out.println("[Exodar] ScheduledExecutorService initialized");

        // Inicializar el ModuleManager
        try {
            moduleManager = new ModuleManager();
            // System.out.println("[Exodar] ModuleManager initialized with " + moduleManager.getModules().size() + " modules");
            // System.out.println("[DEBUG] moduleManager instance: " + moduleManager);
            // System.out.println("[DEBUG] moduleManager class: " + moduleManager.getClass());
            // System.out.println("[DEBUG] moduleManager classloader: " + moduleManager.getClass().getClassLoader());

            // Validate and cleanup UserConfig against current modules
            // This removes entries for modules that no longer exist
            io.github.exodar.config.UserConfig.getInstance().validateAndCleanup(moduleManager);
        } catch (Exception e) {
            // System.out.println("[Exodar] Error initializing ModuleManager: " + e.getMessage());
            e.printStackTrace();
        }

        // Inicializar el ConfigManager
        try {
            configManager = new ConfigManager(moduleManager);
            // System.out.println("[Exodar] ConfigManager initialized");

            // Load Default config on startup (if exists)
            // Users can save their settings to Default for auto-load
            if (configManager.configExists("Default")) {
                configManager.loadConfig("Default");
                System.out.println("[Exodar] Loaded Default config");
            }
        } catch (Exception e) {
            // System.out.println("[Exodar] Error initializing ConfigManager: " + e.getMessage());
            e.printStackTrace();
        }

        EventBus.register(new RenderListener());
        EventBus.register(new io.github.exodar.listener.ClientListener());
        // EventBus.register(io.github.exodar.gui.modern.ModernClickGuiRenderer.getInstance()); // TODO: GUI disabled

        // Start WebSocket server for external GUI communication
        try {
            webSocketServer = new ExodarWebSocketServer(WEBSOCKET_PORT);
            webSocketServer.start();
            System.out.println("[Exodar] WebSocket server started on port " + WEBSOCKET_PORT);
        } catch (Exception e) {
            System.err.println("[Exodar] Failed to start WebSocket server: " + e.getMessage());
            e.printStackTrace();
        }

        // Start HUD render thread for smoother ArrayList/Notifications
        try {
            HudRenderThread.start(moduleManager);
            // System.out.println("[Exodar] HudRenderThread started");
        } catch (Exception e) {
            // System.out.println("[Exodar] Error starting HudRenderThread: " + e.getMessage());
            e.printStackTrace();
        }

        // Start render hooks
        try {
            Minecraft mc = Minecraft.getMinecraft();

            RenderEventBridge.initialize();
            AttackEventBridge.initialize();

            // EntityRenderHook - required for Render3DEvent (ESP, Tracers, Nametags)
            // Also required for Reach/Penetration via getMouseOver proxy
            EntityRenderHook renderHook = new EntityRenderHook(mc);
            renderHook.start();

            // ItemRendererHook - required for custom blocking animations
            ItemRendererHook itemRenderHook = new ItemRendererHook(mc);
            itemRenderHook.start();

            KeyboardHook keyboardHook = new KeyboardHook();
            keyboardHook.start();

            // GuiRenderHook wraps GuiIngame
            GuiRenderHook guiRenderHook = new GuiRenderHook();
            guiRenderHook.start();

            // Initialize Render3DManager to handle ESP modules
            Render3DManager.init(moduleManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize API and auto-authenticate using launcher credentials
        try {
            // System.out.println("[Exodar] Initializing API connection...");
            ExodarAPI api = ExodarAPI.getInstance();

            // Set up command callback to handle remote commands
            api.setCommandCallback(new ExodarAPI.CommandCallback() {
                @Override
                public void onChatCommand(String message) {
                    try {
                        Minecraft mc = Minecraft.getMinecraft();
                        EntityPlayerSP player = getPlayer(mc);
                        if (player != null) {
                            // Send chat message using reflection
                            sendPlayerChatMessage(player, message);
                            // System.out.println("[Exodar] Sent chat: " + message);
                        }
                    } catch (Exception e) {
                        // System.out.println("[Exodar] Chat command error: " + e.getMessage());
                    }
                }

                @Override
                public void onCloseCommand() {
                    try {
                        // System.out.println("[Exodar] Close command received - shutting down game");
                        // Use reflection to call shutdown
                        Method shutdownMethod = null;
                        for (Method m : Minecraft.class.getDeclaredMethods()) {
                            if (m.getParameterCount() == 0 && m.getReturnType() == void.class) {
                                String name = m.getName().toLowerCase();
                                if (name.contains("shutdown") || name.contains("stop") || name.equals("m")) {
                                    shutdownMethod = m;
                                    break;
                                }
                            }
                        }
                        if (shutdownMethod != null) {
                            shutdownMethod.setAccessible(true);
                            shutdownMethod.invoke(Minecraft.getMinecraft());
                        } else {
                            // Fallback: System.exit
                            System.exit(0);
                        }
                    } catch (Exception e) {
                        // System.out.println("[Exodar] Close command error: " + e.getMessage());
                        System.exit(0);
                    }
                }

                @Override
                public void onKickCommand(String reason) {
                    try {
                        Minecraft mc = Minecraft.getMinecraft();
                        // Disconnect from server using reflection
                        Object world = theWorldField != null ? theWorldField.get(mc) : null;
                        if (world != null) {
                            for (Method m : world.getClass().getDeclaredMethods()) {
                                if (m.getParameterCount() == 0 && m.getName().contains("sendQuitting")) {
                                    m.setAccessible(true);
                                    m.invoke(world);
                                    break;
                                }
                            }
                        }
                        // System.out.println("[Exodar] Kicked: " + reason);
                    } catch (Exception e) {
                        // System.out.println("[Exodar] Kick command error: " + e.getMessage());
                    }
                }

                @Override
                public void onCustomCommand(String command) {
                    try {
                        Minecraft mc = Minecraft.getMinecraft();
                        EntityPlayerSP player = getPlayer(mc);
                        if (player != null && command.startsWith("/")) {
                            // Execute as command using reflection
                            sendPlayerChatMessage(player, command);
                        }
                        // System.out.println("[Exodar] Custom command: " + command);
                    } catch (Exception e) {
                        // System.out.println("[Exodar] Custom command error: " + e.getMessage());
                    }
                }
            });

            api.initFromLauncherCredentials().thenAccept(success -> {
                if (success) {
                    // System.out.println("[Exodar] API authentication successful!");
                    // Start heartbeat after successful auth
                    api.startHeartbeat();

                    // Set initial session info with Minecraft player data
                    try {
                        Minecraft mc = Minecraft.getMinecraft();
                        if (mc != null && mc.getSession() != null) {
                            String playerName = mc.getSession().getUsername();
                            String playerUuid = mc.getSession().getPlayerID();
                            String serverIp = null;
                            String serverName = null;

                            // Check if already connected to a server
                            if (mc.getCurrentServerData() != null) {
                                serverIp = mc.getCurrentServerData().serverIP;
                                serverName = mc.getCurrentServerData().serverName;
                            }

                            api.updateSessionInfo(playerName, playerUuid, serverIp, serverName);
                            // System.out.println("[Exodar] Initial session info set: " + playerName);
                        }
                    } catch (Exception e) {
                        // System.out.println("[Exodar] Could not get initial session info: " + e.getMessage());
                    }
                } else {
                    // Authentication failed - kill the game
                    // System.out.println("[Exodar] API authentication failed - closing game");
                    try {
                        // Give a small delay before killing (let any UI render)
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
                    System.exit(0);
                }
            });
        } catch (Exception e) {
            // System.out.println("[Exodar] Error initializing API: " + e.getMessage());
            e.printStackTrace();
        }

        // Try to send message to chat
        try {
            Minecraft mc = Minecraft.getMinecraft();
            // System.out.println("[Exodar] Minecraft instance: " + mc);

            EntityPlayerSP player = getPlayer(mc);
            // System.out.println("[Exodar] Player instance: " + player);

            if (player != null) {
                // sendChatMessage(player, "§b§l[Exodar] §fClient loaded successfully!");
                // System.out.println("[Exodar] Chat message disabled to avoid errors");
            } else {
                // System.out.println("[Exodar] Player not loaded yet, will show watermark when in-game");
            }
        } catch (Exception e) {
            // System.out.println("[Exodar] Error in onLoad: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("done");
    }

    // warning called from c++ thread
    public static void onUnload()
    {
        // System.out.println("[Exodar] ========================================");
        // System.out.println("[Exodar] Unloading from Minecraft 1.8.9");
        // System.out.println("[Exodar] ========================================");

        try {
            // CRITICAL: Disable all modules and stop threads before unloading
            if (moduleManager != null) {
                // System.out.println("[Exodar] Cleaning up ModuleManager...");
                moduleManager.cleanup();
            }

            // Stop API heartbeat
            try {
                ExodarAPI.getInstance().stopHeartbeat();
            } catch (Exception ignored) {}

            // Stop WebSocket server
            try {
                if (webSocketServer != null) {
                    webSocketServer.stop(1000);
                    webSocketServer = null;
                }
            } catch (Exception ignored) {}

            // Stop HUD render thread
            try {
                HudRenderThread.stop();
            } catch (Exception ignored) {}

            // Stop EntityRenderHook
            try {
                EntityRenderHook.cleanup();
                Render3DManager.cleanup();
            } catch (Exception ignored) {}

            // Stop ItemRendererHook
            try {
                ItemRendererHook.cleanup();
            } catch (Exception ignored) {}

            // Shutdown AttackEventBridge
            try {
                AttackEventBridge.shutdown();
            } catch (Exception ignored) {}

            // Shutdown executor service
            if (executor != null) {
                executor.shutdown();
            }

            // =====================================================
            // CLEAR ALL STATIC REFERENCES TO ALLOW GC
            // This is critical for clean re-injection
            // =====================================================

            // Core references
            moduleManager = null;
            configManager = null;
            executor = null;

            // Reflection caches
            playerField = null;
            addChatMethod = null;
            thePlayerField = null;
            theWorldField = null;
            cachedNetworkManager = null;
            cachedSendPacketMethod = null;
            networkReflectionInitialized = false;

            // Module caches
            blinkModuleCache = null;
            reachModuleCache = null;
            penetrationModuleCache = null;

            // State arrays and lists
            cachedEnabledModules.clear();
            cachedEnabledModules = new java.util.ArrayList<>();
            lastModuleStateHash = 0;

            // Session tracking
            lastServerIp = null;
            wasConnectedToServer = false;
            wasMiddleMousePressed = false;

            // Debug flags (reset for next injection)
            hookMessageShown = false;
            renderHookShown = false;
            renderWorldDebugShown = false;
            packetHookDebugShown = false;
            receivePacketHookDebugShown = false;
            nametagHookDebugShown = false;
            reachHookDebugShown = false;
            preUpdateHookShown = false;
            preMotionHookShown = false;
            moveInputHookShown = false;
            blockShapeHookShown = false;
            playerJumpHookShown = false;
            safeWalkHookShown = false;

            // PreMotion state
            savedYaw = 0;
            savedPitch = 0;
            savedPrevYaw = 0;
            savedPrevPitch = 0;

            // PlayerJump state
            modifiedJumpMotion = 0;
            jumpMotionModified = false;

            // Counters
            tickCounter = 0;
            renderCounter = 0;
            renderWorldCounter = 0;

            // Clear other static caches in other classes
            try {
                io.github.exodar.ui.ModuleAnimation.clearAll();
            } catch (Exception ignored) {}

            try {
                io.github.exodar.ui.ModuleNotification.clearAll();
            } catch (Exception ignored) {}

            // Request garbage collection (hint to JVM)
            System.gc();

            // System.out.println("[Exodar] ========================================");
            // System.out.println("[Exodar] Unload complete - Safe to reinject");
            // System.out.println("[Exodar] ========================================");

        } catch (Exception e) {
            // System.out.println("[Exodar] ERROR in onUnload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int tickCounter = 0;
    private static boolean hookMessageShown = false;

    // ===============================================
    // TICK START HOOK - ON_ENTRY (fires at start of runTick)
    // Port from teamoandre mixin system
    // ===============================================
    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/client/Minecraft",
            targetMethodName = "runTick",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onTickStart(Canceler canceler, Minecraft mc) {
        // Fire TickStartEvent for modules that need early tick processing
        EventBus.post(new TickStartEvent());
    }

    // ===============================================
    // PRE-UPDATE HOOK - ON_ENTRY (fires at start of player tick)
    // Port from teamoandre MixinEntityPlayerSP
    // This is where PreUpdateEvent is fired - BEFORE onUpdateWalkingPlayer
    // ===============================================
    private static boolean preUpdateHookShown = false;

    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "onUpdate",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onPreUpdate(Canceler canceler, EntityPlayerSP player) {
        if (!preUpdateHookShown) {
            System.out.println("[Exodar] PRE-UPDATE HOOK ACTIVATED!");
            preUpdateHookShown = true;
        }

        try {
            // Fire PreUpdateEventAux (identical to cc.unknown PreUpdateEvent)
            // This is the correct timing - at start of onUpdate, not onUpdateWalkingPlayer
            PreUpdateEventAux preUpdateAux = new PreUpdateEventAux();
            EventBus.post(preUpdateAux);
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ===============================================
    // PRE-MOTION HOOK - ON_ENTRY (fires before movement packet sent)
    // Port from teamoandre mixin system
    // Used for Silent Aim - allows modules to modify rotation before C03 packet
    // ===============================================
    private static boolean preMotionHookShown = false;
    private static float savedYaw = 0;
    private static float savedPitch = 0;
    private static float savedPrevYaw = 0;
    private static float savedPrevPitch = 0;

    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "onUpdateWalkingPlayer",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onPreMotion(Canceler canceler, EntityPlayerSP player) {
        if (!preMotionHookShown) {
            System.out.println("[Exodar] PRE-MOTION HOOK ACTIVATED!");
            preMotionHookShown = true;
        }

        try {
            // Create and fire PreMotionEvent
            PreMotionEvent event = new PreMotionEvent(
                player.rotationYaw,
                player.rotationPitch,
                player.posX,
                player.posY,
                player.posZ,
                player.onGround
            );
            EventBus.post(event);

            // If event was modified, temporarily change player values
            // These will be used when C03 packet is constructed
            if (event.isModified()) {
                // Save original values to restore in PostMotion
                savedYaw = player.rotationYaw;
                savedPitch = player.rotationPitch;
                savedPrevYaw = player.prevRotationYaw;
                savedPrevPitch = player.prevRotationPitch;

                // Apply modified values
                player.rotationYaw = event.getYaw();
                player.rotationPitch = event.getPitch();
                player.prevRotationYaw = event.getYaw();
                player.prevRotationPitch = event.getPitch();

                // Position and onGround can also be modified
                if (event.getX() != player.posX) player.posX = event.getX();
                if (event.getY() != player.posY) player.posY = event.getY();
                if (event.getZ() != player.posZ) player.posZ = event.getZ();
                if (event.isOnGround() != player.onGround) player.onGround = event.isOnGround();
            } else {
                savedYaw = 0;
                savedPitch = 0;
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ===============================================
    // POST-MOTION HOOK - ON_RETURN_THROW (fires after movement packet sent)
    // Port from teamoandre mixin system
    // Used to restore original rotation after Silent Aim
    // ===============================================
    @EventHandler(type=ON_RETURN_THROW,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "onUpdateWalkingPlayer",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onPostMotion(Thrower thrower, EntityPlayerSP player) {
        try {
            // Restore original rotation if it was modified in PreMotion
            if (savedYaw != 0 || savedPitch != 0) {
                player.rotationYaw = savedYaw;
                player.rotationPitch = savedPitch;
                player.prevRotationYaw = savedPrevYaw;
                player.prevRotationPitch = savedPrevPitch;
            }

            // Fire PostMotionEvent
            PostMotionEvent event = new PostMotionEvent(
                player.rotationYaw,
                player.rotationPitch,
                player.posX,
                player.posY,
                player.posZ,
                player.onGround
            );
            EventBus.post(event);
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ===============================================
    // MOVE INPUT HOOK - ON_RETURN_THROW (fires after movement input processed)
    // Port from teamoandre mixin system
    // Allows modules to modify moveForward, moveStrafing, sneak, jump
    // ===============================================
    private static boolean moveInputHookShown = false;

    @EventHandler(type=ON_RETURN_THROW,
            targetClass = "net/minecraft/util/MovementInputFromOptions",
            targetMethodName = "updatePlayerMoveState",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onMoveInput(Thrower thrower, MovementInputFromOptions input) {
        if (!moveInputHookShown) {
            System.out.println("[Exodar] MOVE INPUT HOOK ACTIVATED!");
            moveInputHookShown = true;
        }

        try {
            // Fire MoveInputEvent with current values
            MoveInputEvent event = new MoveInputEvent(
                input.moveForward,
                input.moveStrafe,
                input.sneak,
                input.jump
            );
            EventBus.post(event);

            // Fire MoveInputEventAux (identical to cc.unknown)
            MoveInputEventAux eventAux = new MoveInputEventAux(
                input.moveForward,
                input.moveStrafe,
                input.jump,
                input.sneak
            );
            EventBus.post(eventAux);

            // Apply modifications from both events (aux takes priority for sneak)
            input.moveForward = eventAux.getForward();
            input.moveStrafe = eventAux.getStrafe();
            input.jump = eventAux.isJump();
            input.sneak = eventAux.isSneak();
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ===============================================
    // BLOCK SHAPE HOOK - ON_RETURN_THROW (fires after collision box calculated)
    // Port from LiquidBounce mixin system
    // Allows modules to modify block collision shapes (e.g., shrink for Spider)
    // Note: For methods returning values, the return value is the first parameter
    // and the hook must also return the (potentially modified) value
    // ===============================================
    private static boolean blockShapeHookShown = false;

    @EventHandler(type=ON_RETURN_THROW,
            targetClass = "net/minecraft/block/Block",
            targetMethodName = "getCollisionBoundingBox",
            targetMethodDescriptor = "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/util/AxisAlignedBB;",
            targetMethodIsStatic = false)
    public static AxisAlignedBB onBlockShape(AxisAlignedBB returnValue, Thrower thrower, Block block, World world, BlockPos pos, IBlockState state) {
        if (!blockShapeHookShown) {
            System.out.println("[Exodar] BLOCK SHAPE HOOK ACTIVATED!");
            blockShapeHookShown = true;
        }

        try {
            // Only process if we have an active player and a valid bounding box
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || returnValue == null) return returnValue;

            // Fire BlockShapeEvent with the original collision box
            BlockShapeEvent event = new BlockShapeEvent(pos, returnValue);
            EventBus.post(event);

            // Return the (potentially modified) shape
            return event.getShape();

        } catch (Exception e) {
            // Silent fail - return original value
            return returnValue;
        }
    }

    // ===============================================
    // PLAYER JUMP HOOK - ON_ENTRY (fires when player jumps)
    // Port from LiquidBounce mixin system
    // Allows modules to modify jump height
    // ===============================================
    private static boolean playerJumpHookShown = false;
    private static float modifiedJumpMotion = 0;
    private static boolean jumpMotionModified = false;

    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/entity/EntityLivingBase",
            targetMethodName = "jump",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onPlayerJump(Canceler canceler, EntityLivingBase entity) {
        if (!playerJumpHookShown) {
            System.out.println("[Exodar] PLAYER JUMP HOOK ACTIVATED!");
            playerJumpHookShown = true;
        }

        try {
            // Only process for the local player
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || entity != mc.thePlayer) return;

            // Fire PlayerJumpEvent with default jump motion
            PlayerJumpEvent event = new PlayerJumpEvent(0.42f);
            EventBus.post(event);

            // If motion was modified, store it for post-processing
            if (event.getMotion() != 0.42f) {
                modifiedJumpMotion = event.getMotion();
                jumpMotionModified = true;
            } else {
                jumpMotionModified = false;
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    @EventHandler(type=ON_RETURN_THROW,
            targetClass = "net/minecraft/entity/EntityLivingBase",
            targetMethodName = "jump",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onPlayerJumpPost(Thrower thrower, EntityLivingBase entity) {
        try {
            // Only process for the local player
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || entity != mc.thePlayer) return;

            // Apply modified jump motion after the vanilla jump
            if (jumpMotionModified && modifiedJumpMotion != 0.42f) {
                mc.thePlayer.motionY = modifiedJumpMotion;
                jumpMotionModified = false;
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ===============================================
    // SAFE WALK HOOK - ON_RETURN_THROW (intercepts isSneaking for safe walk)
    // Port from Raven XD MixinEntity
    // Allows modules to prevent walking off edges without holding sneak
    // When SafeWalk returns true, the game thinks player is sneaking for edge detection
    // ===============================================
    private static boolean safeWalkHookShown = false;

    @EventHandler(type=ON_RETURN_THROW,
            targetClass = "net/minecraft/entity/Entity",
            targetMethodName = "isSneaking",
            targetMethodDescriptor = "()Z",
            targetMethodIsStatic = false)
    public static boolean onIsSneaking(boolean returnValue, Thrower thrower, Entity entity) {
        if (!safeWalkHookShown) {
            System.out.println("[Exodar] SAFE WALK HOOK ACTIVATED!");
            safeWalkHookShown = true;
        }

        try {
            // Only process for the local player
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || entity != mc.thePlayer) return returnValue;

            // Fire SafeWalkEvent with current sneaking state
            SafeWalkEvent event = new SafeWalkEvent(returnValue);
            EventBus.post(event);

            // Return the (potentially modified) safe walk state
            return event.isSafeWalk();

        } catch (Exception e) {
            // Silent fail - return original value
            return returnValue;
        }
    }

    // ===============================================
    // ATTACK EVENT - Now handled by EntityPlayerPatcher (1:1 Sakura bytecode injection)
    // The patcher injects AttackEvent creation + EventBus.post directly into
    // EntityPlayer.attackTargetEntityWithCurrentItem and replaces the hardcoded
    // 0.6 slowdown with event.getSlowdownFactor() and setSprinting(false) with
    // event.isAllowSprint() - TRUE 1:1 with Sakura mixin behavior
    // ===============================================

    // ===============================================
    // TICK END HOOK - ON_RETURN_THROW (fires at end of runTick)
    // ===============================================
    @EventHandler(type=ON_RETURN_THROW,
            targetClass = "net/minecraft/client/Minecraft",
            targetMethodName = "runTick",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onClientTick(Thrower thrower, Minecraft mc)
    {
        // Debug: mostrar que el hook funciona (solo una vez)
        if (!hookMessageShown) {
            // System.out.println("[Exodar] ====================================");
            // System.out.println("[Exodar] HOOK ACTIVADO - onClientTick funciona!");
            // System.out.println("[Exodar] ====================================");
            hookMessageShown = true;
        }

        tickCounter++;

        try {
            // Verificar si el jugador y el mundo están cargados usando reflection
            initMinecraftFields();
            if (thePlayerField == null || theWorldField == null) return;

            Object player = thePlayerField.get(mc);
            Object world = theWorldField.get(mc);

            if (player == null || world == null) {
                return;
            }

            // Install custom player renderers (once, when world is loaded)
            if (!RendererInstaller.isInstalled()) {
                try {
                    RendererInstaller.installCustomRenderers();
                } catch (Throwable t) {
                    // System.out.println("[Exodar] Error installing custom renderers: " + t.getMessage());
                }
            }

            // Actualizar módulos
            if (moduleManager != null) {
                moduleManager.onUpdate();

                // Fire KeyPressEvent for key events (only if no GUI is open)
                boolean guiOpen = mc.currentScreen != null;
                boolean chatOpen = mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null && mc.ingameGUI.getChatGUI().getChatOpen();

                // Process keyboard events and fire KeyPressEvent
                // Only process module keybinds when no GUI/chat is open
                // BUT always allow menu keybind (to close GUI)
                while (Keyboard.next()) {
                    int keyCode = Keyboard.getEventKey();
                    boolean pressed = Keyboard.getEventKeyState();
                    if (keyCode != 0 && pressed) {
                        // Always allow menu keybind (to open/close GUI)
                        if (keyCode == io.github.exodar.ui.ArrayListConfig.menuKeybind) {
                            EventBus.post(new io.github.exodar.event.KeyPressEvent(keyCode, pressed));
                        }
                        // Only allow other keybinds when no GUI/chat is open
                        else if (!guiOpen && !chatOpen) {
                            EventBus.post(new io.github.exodar.event.KeyPressEvent(keyCode, pressed));
                        }
                    }
                }

                // HOLD binds need polling (not event-based) since they check continuous state
                // Only process hold binds when no GUI/chat is open (same as toggle binds)
                if (!guiOpen && !chatOpen) {
                    for (io.github.exodar.module.Module module : moduleManager.getModules()) {
                        int holdBind = module.getHoldBind();
                        if (holdBind != 0) {
                            boolean isPressed = isKeyOrMousePressed(holdBind);
                            if (isPressed && !module.isEnabled()) {
                                module.setEnabled(true);
                            } else if (!isPressed && module.isEnabled()) {
                                module.setEnabled(false);
                            }
                        }
                    }
                } else {
                    // GUI/chat is open - disable any modules that were enabled by hold bind
                    for (io.github.exodar.module.Module module : moduleManager.getModules()) {
                        int holdBind = module.getHoldBind();
                        if (holdBind != 0 && module.isEnabled()) {
                            // Only disable if this module was activated via hold bind
                            // (no toggle bind or toggle bind not pressed)
                            int toggleBind = module.getToggleBind();
                            if (toggleBind == 0) {
                                module.setEnabled(false);
                            }
                        }
                    }
                }

                // ===============================================
                // MIDDLE-CLICK FRIEND TOGGLE
                // Only works when NO GUI is open (not in inventory, ClickGUI, etc.)
                // ===============================================
                boolean middlePressed = org.lwjgl.input.Mouse.isButtonDown(2);
                if (middlePressed && !wasMiddleMousePressed && mc.currentScreen == null) {
                    // Middle mouse just pressed (and no GUI open) - try to add/remove friend
                    try {
                        // Get entity the player is looking at
                        net.minecraft.util.MovingObjectPosition mop = mc.objectMouseOver;
                        if (mop != null && mop.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.ENTITY) {
                            net.minecraft.entity.Entity entity = mop.entityHit;
                            if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                                String targetName = entity.getName();
                                boolean added = io.github.exodar.friend.FriendManager.getInstance().toggleFriend(targetName);
                                if (added) {
                                    io.github.exodar.ui.ModuleNotification.addNotification("+ " + targetName, true);
                                } else {
                                    io.github.exodar.ui.ModuleNotification.addNotification("- " + targetName, false);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                wasMiddleMousePressed = middlePressed;
            }

            // NOTE: INSERT key detection moved to onRenderGameOverlay() to avoid conflicts
            // and allow proper toggle functionality

            // ===============================================
            // SESSION INFO TRACKING FOR API HEARTBEAT
            // ===============================================
            try {
                boolean isConnectedToServer = mc.getCurrentServerData() != null;
                String currentServerIp = isConnectedToServer ? mc.getCurrentServerData().serverIP : null;

                // Detect server connection change
                if (isConnectedToServer && !wasConnectedToServer) {
                    // Just connected to a server
                    String playerName = null;
                    String playerUuid = null;

                    try {
                        // Get player name from session
                        if (mc.getSession() != null) {
                            playerName = mc.getSession().getUsername();
                            playerUuid = mc.getSession().getPlayerID();
                        }
                    } catch (Exception ignored) {}

                    ExodarAPI.getInstance().updateSessionInfo(
                        playerName,
                        playerUuid,
                        currentServerIp,
                        mc.getCurrentServerData().serverName
                    );
                    lastServerIp = currentServerIp;
                    // System.out.println("[Exodar] Connected to server: " + currentServerIp);
                } else if (!isConnectedToServer && wasConnectedToServer) {
                    // Just disconnected from server
                    ExodarAPI.getInstance().clearSessionInfo();
                    lastServerIp = null;
                    // System.out.println("[Exodar] Disconnected from server");
                } else if (isConnectedToServer && currentServerIp != null && !currentServerIp.equals(lastServerIp)) {
                    // Changed servers
                    String playerName = null;
                    String playerUuid = null;

                    try {
                        if (mc.getSession() != null) {
                            playerName = mc.getSession().getUsername();
                            playerUuid = mc.getSession().getPlayerID();
                        }
                    } catch (Exception ignored) {}

                    ExodarAPI.getInstance().updateSessionInfo(
                        playerName,
                        playerUuid,
                        currentServerIp,
                        mc.getCurrentServerData().serverName
                    );
                    lastServerIp = currentServerIp;
                    // System.out.println("[Exodar] Changed server to: " + currentServerIp);
                }

                wasConnectedToServer = isConnectedToServer;
            } catch (Exception e) {
                // Silent fail for session tracking
            }

            // Fire TickEndEvent at the end of tick processing
            EventBus.post(new TickEndEvent());

        } catch (Exception e) {
            // System.out.println("[Exodar] ERROR in onClientTick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int renderCounter = 0;
    private static boolean renderHookShown = false;


    // TODO: NoHurtCam - Necesita un tipo de hook diferente (no ON_RETURN_THROW)
    // ON_RETURN_THROW se ejecuta DESPUÉS del método, no podemos cancelarlo
    // Necesitaría un hook BEFORE o modificar los valores dentro del método

    private static int renderWorldCounter = 0;
    private static boolean renderWorldDebugShown = false;

    // NOTE: onRenderWorld hook removed - ESP modules now use Render3DEvent from
    // EntityRenderHook proxy which fires at the correct point in renderWorldPass
    // (after world/entities/particles, before hand rendering)

    // Hook into GuiIngame.renderGameOverlay - DISABLED, using EntityRenderHook instead
    // EntityRenderHook is needed for Render3DEvent (ESP, Tracers, Nametags)
    /*@EventHandler(type=ON_RETURN_THROW,
            targetClass = "net/minecraft/client/gui/GuiIngame",
            targetMethodName = "renderGameOverlay",
            targetMethodDescriptor = "(F)V",
            targetMethodIsStatic = false)*/
    public static void onRenderGameOverlay(Thrower thrower, GuiIngame guiIngame, float partialTicks)
    {
        renderCounter++;
        EventBus.post(new Render2DEvent(partialTicks));
    }

    /**
     * Get the ModuleManager instance
     */
    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * Get the ConfigManager instance
     */
    public static ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Initialize API session with token from launcher
     * Called by launcher after successful authentication
     * @param token The authentication token
     * @param hwidHash The HWID hash
     * @param username The username (for fallback if API call fails)
     */
    public static void initializeSession(String token, String hwidHash, String username) {
        // System.out.println("[Exodar] Initializing API session for user: " + username);

        BuildInfo info = BuildInfo.getInstance();
        info.setSession(token, hwidHash);

        // Set default build info with username (will be updated from API)
        info.update("release", "Release Build", username,
            new java.text.SimpleDateFormat("ddMMyy").format(new java.util.Date()),
            true, true, false);

        // Fetch build info from API asynchronously
        ExodarAPI.getInstance().fetchBuildInfo().thenAccept(success -> {
            if (success) {
                // System.out.println("[Exodar] Build info loaded from API");
            } else {
                // System.out.println("[Exodar] Using default build info");
            }
        });
    }

    /**
     * Get BuildInfo instance
     */
    public static BuildInfo getBuildInfo() {
        return BuildInfo.getInstance();
    }

    /**
     * Get the ScheduledExecutorService for scheduled tasks
     */
    public static ScheduledExecutorService getExecutor() {
        return executor;
    }

    // ===============================================
    // PACKET INTERCEPTION HOOK - ON_ENTRY
    // ===============================================

    private static boolean packetHookDebugShown = false;
    private static Blink blinkModuleCache = null;

    /**
     * Hook into NetworkManager.sendPacket to intercept outgoing packets
     * This allows Blink module to queue packets and release them later
     * Also handles chat commands starting with "."
     */
    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/network/NetworkManager",
            targetMethodName = "sendPacket",
            targetMethodDescriptor = "(Lnet/minecraft/network/Packet;)V",
            targetMethodIsStatic = false)
    public static void onSendPacket(Canceler canceler, NetworkManager networkManager, Packet packet) {
        // Debug: show that the hook works (only once)
        if (!packetHookDebugShown) {
            // System.out.println("[Exodar] ====================================");
            // System.out.println("[Exodar] PACKET HOOK ACTIVATED - onSendPacket works!");
            // System.out.println("[Exodar] ====================================");
            packetHookDebugShown = true;
        }

        try {
            // =============================================
            // CHAT COMMAND INTERCEPTION
            // =============================================
            if (packet instanceof C01PacketChatMessage) {
                C01PacketChatMessage chatPacket = (C01PacketChatMessage) packet;
                String message = chatPacket.getMessage();

                // Check if this is a command (starts with ".")
                if (message != null && message.startsWith(".")) {
                    // Handle the command
                    boolean handled = CommandManager.handleCommand(message);
                    if (handled) {
                        // Cancel the packet - don't send to server
                        canceler.cancel = true;
                        return;
                    }
                }
            }

            // =============================================
            // BEDROCK SPOOF - Intercept brand packets
            // =============================================
            BedrockSpoof bedrockSpoof = BedrockSpoof.getInstance();
            if (bedrockSpoof != null && bedrockSpoof.isEnabled()) {
                bedrockSpoof.onPacketSend(packet);
            }

            // Check if Blink, Ping, TimerRange, LagRange, or AutoBlock is bypassing (sending queued packets directly)
            if (Blink.isSendingDirectPacket() || TimerRange.isSendingDirectPacket() ||
                io.github.exodar.module.modules.player.Ping.isSendingDirectPacket() ||
                LagRange.isSendingDirectPacket() ||
                io.github.exodar.module.modules.combat.AutoBlock.isSendingBufferedPacket()) {
                return; // Allow packet through
            }

            // =============================================
            // ATTACK DETECTION FOR AUTOBLOCK, BACKTRACK, BLOCKHIT, PING
            // =============================================
            if (packet instanceof C02PacketUseEntity) {
                C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;
                if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    try {
                        Minecraft mc = Minecraft.getMinecraft();
                        if (mc.theWorld != null) {
                            net.minecraft.entity.Entity entity = useEntity.getEntityFromWorld(mc.theWorld);
                            if (moduleManager != null) {
                                // Notify Backtrack about the attack (players only)
                                if (entity instanceof EntityPlayer) {
                                    io.github.exodar.module.Module backtrack = moduleManager.getModuleByName("Backtrack");
                                    if (backtrack != null && backtrack.isEnabled() && backtrack instanceof Backtrack) {
                                        ((Backtrack) backtrack).onAttackEntity((EntityPlayer) entity);
                                    }
                                }

                                // Notify BlockHit about the attack (any entity)
                                io.github.exodar.module.Module blockHit = moduleManager.getModuleByName("BlockHit");
                                if (blockHit != null && blockHit.isEnabled() && blockHit instanceof io.github.exodar.module.modules.combat.BlockHit) {
                                    ((io.github.exodar.module.modules.combat.BlockHit) blockHit).onAttackEntity(entity);
                                }

                                // Notify STap about the attack
                                io.github.exodar.module.Module stapModule = moduleManager.getModuleByName("STap");
                                if (stapModule != null && stapModule.isEnabled() && stapModule instanceof STap) {
                                    ((STap) stapModule).onAttack();
                                }

                                // Notify WTap about the attack
                                io.github.exodar.module.Module wtapModule = moduleManager.getModuleByName("Wtap");
                                if (wtapModule != null && wtapModule.isEnabled() && wtapModule instanceof io.github.exodar.module.modules.combat.WTap) {
                                    ((io.github.exodar.module.modules.combat.WTap) wtapModule).onAttack();
                                }

                                // Notify MoreKB about the attack
                                io.github.exodar.module.Module morekbModule = moduleManager.getModuleByName("MoreKB");
                                if (morekbModule != null && morekbModule.isEnabled() && morekbModule instanceof MoreKB) {
                                    ((MoreKB) morekbModule).onAttack(entity);
                                }

                                // Notify Criticals about the attack
                                if (entity instanceof EntityLivingBase) {
                                    io.github.exodar.module.Module criticalsModule = moduleManager.getModuleByName("Criticals");
                                    if (criticalsModule != null && criticalsModule.isEnabled() && criticalsModule instanceof Criticals) {
                                        ((Criticals) criticalsModule).onAttack((EntityLivingBase) entity);
                                    }
                                }

                                // Notify NoVelocity about the attack (for Intave mode slowdown)
                                io.github.exodar.module.Module noVelocityModule = moduleManager.getModuleByName("NoVelocity");
                                if (noVelocityModule != null && noVelocityModule.isEnabled() && noVelocityModule instanceof io.github.exodar.module.modules.combat.NoVelocity) {
                                    ((io.github.exodar.module.modules.combat.NoVelocity) noVelocityModule).onAttackEntity(entity);
                                }

                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Get Blink module (no cache - always get fresh reference)
            Blink blinkModule = null;
            if (moduleManager != null) {
                io.github.exodar.module.Module m = moduleManager.getModuleByName("Blink");
                if (m instanceof Blink) {
                    blinkModule = (Blink) m;
                }
            }

            // If Blink is enabled, check if we should intercept the packet
            if (blinkModule != null && blinkModule.isEnabled()) {
                boolean allow = blinkModule.shouldAllowPacket(packet);
                if (!allow) {
                    // Cancel the packet - Blink has queued it
                    canceler.cancel = true;
                    return;
                }
            }

            // Check LagRange module for outgoing packets
            if (moduleManager != null) {
                io.github.exodar.module.Module lagRangeModule = moduleManager.getModuleByName("LagRange");
                if (lagRangeModule != null && lagRangeModule.isEnabled() && lagRangeModule instanceof LagRange) {
                    LagRange lagRange = (LagRange) lagRangeModule;
                    boolean allow = lagRange.shouldAllowPacket(packet);
                    if (!allow) {
                        canceler.cancel = true;
                        return;
                    }
                }
            }

            // Check Ping module for outgoing packets
            if (moduleManager != null) {
                io.github.exodar.module.Module pingModule = moduleManager.getModuleByName("Ping");
                if (pingModule != null && pingModule.isEnabled() && pingModule instanceof io.github.exodar.module.modules.player.Ping) {
                    io.github.exodar.module.modules.player.Ping ping = (io.github.exodar.module.modules.player.Ping) pingModule;
                    boolean allow = ping.shouldAllowOutgoingPacket(packet);
                    if (!allow) {
                        canceler.cancel = true;
                        return;
                    }
                }
            }

            // Also notify other modules that want packet events
            if (moduleManager != null) {
                for (io.github.exodar.module.Module module : moduleManager.getModules()) {
                    if (module.isEnabled() && module != blinkModuleCache) {
                        if (!module.onSendPacket(packet)) {
                            canceler.cancel = true;
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail to avoid crashing networking
            // if (packetHookDebugShown) {
            //     // System.out.println("[Exodar] Error in onSendPacket: " + e.getMessage());
            // }
        }
    }

    // ===============================================
    // INCOMING PACKET HOOK - ON_ENTRY
    // ===============================================

    private static boolean receivePacketHookDebugShown = false;

    /**
     * Hook into NetworkManager.channelRead0 to intercept incoming packets
     * This allows Blink module to queue incoming packets as well
     */
    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/network/NetworkManager",
            targetMethodName = "channelRead0",
            targetMethodDescriptor = "(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
            targetMethodIsStatic = false)
    public static void onReceivePacket(Canceler canceler, NetworkManager networkManager, ChannelHandlerContext ctx, Packet packet) {
        // Debug: show that the hook works (only once)
        if (!receivePacketHookDebugShown) {
            // System.out.println("[Exodar] ====================================");
            // System.out.println("[Exodar] RECEIVE PACKET HOOK ACTIVATED!");
            // System.out.println("[Exodar] ====================================");
            receivePacketHookDebugShown = true;
        }

        try {
            // Check if Blink, Ping, TimerRange, or Backtrack is bypassing
            if (Blink.isSendingDirectPacket() || Blink.isProcessingIncomingPacket() ||
                TimerRange.isSendingDirectPacket() || Backtrack.isProcessingDirectPacket() ||
                io.github.exodar.module.modules.player.Ping.isSendingDirectPacket()) {
                return; // Allow packet through
            }

            // Get Blink module (no cache - always get fresh reference)
            Blink blinkModule = null;
            if (moduleManager != null) {
                io.github.exodar.module.Module m = moduleManager.getModuleByName("Blink");
                if (m instanceof Blink) {
                    blinkModule = (Blink) m;
                }
            }

            // If Blink is enabled, check if we should intercept the packet
            if (blinkModule != null && blinkModule.isEnabled()) {
                boolean allow = blinkModule.shouldAllowIncomingPacket(packet);
                if (!allow) {
                    // Cancel the packet - Blink has queued it
                    canceler.cancel = true;
                    return;
                }
            }

            // Check Ping module for incoming packets
            if (moduleManager != null) {
                io.github.exodar.module.Module pingModule = moduleManager.getModuleByName("Ping");
                if (pingModule != null && pingModule.isEnabled() && pingModule instanceof io.github.exodar.module.modules.player.Ping) {
                    io.github.exodar.module.modules.player.Ping ping = (io.github.exodar.module.modules.player.Ping) pingModule;
                    boolean allow = ping.shouldAllowIncomingPacket(packet);
                    if (!allow) {
                        canceler.cancel = true;
                        return;
                    }
                }
            }

            // LunarUnlock disabled - requires Java Agent for bytecode patching
            // if (moduleManager != null) {
            //     io.github.exodar.module.Module lunarModule = moduleManager.getModuleByName("LunarUnlock");
            //     if (lunarModule != null && lunarModule.isEnabled() && lunarModule instanceof io.github.exodar.module.modules.misc.LunarUnlock) {
            //         io.github.exodar.module.modules.misc.LunarUnlock lunar = (io.github.exodar.module.modules.misc.LunarUnlock) lunarModule;
            //         if (lunar.shouldBlockPacket(packet)) {
            //             canceler.cancel = true;
            //             return;
            //         }
            //     }
            // }

            // Also notify other modules that want packet events
            if (moduleManager != null) {
                for (io.github.exodar.module.Module module : moduleManager.getModules()) {
                    if (module.isEnabled() && module != blinkModule) {
                        if (!module.onReceivePacket(packet)) {
                            canceler.cancel = true;
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail to avoid crashing networking
            // if (receivePacketHookDebugShown) {
            //     // System.out.println("[Exodar] Error in onReceivePacket: " + e.getMessage());
            // }
        }
    }

    // ===============================================
    // VANILLA NAMETAG CANCELLATION HOOK
    // ===============================================

    private static boolean nametagHookDebugShown = false;

    /**
     * Hook into Render.renderLivingLabel to cancel vanilla nametags
     * Method signature: renderLivingLabel(Entity, String, double, double, double, int)
     */
    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/client/renderer/entity/Render",
            targetMethodName = "renderLivingLabel",
            targetMethodDescriptor = "(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V",
            targetMethodIsStatic = false)
    public static void onRenderLivingLabel(Canceler canceler, net.minecraft.client.renderer.entity.Render render, net.minecraft.entity.Entity entity, String str, double x, double y, double z, int maxDistance) {
        if (!nametagHookDebugShown) {
            // System.out.println("[Exodar] ====================================");
            // System.out.println("[Exodar] NAMETAG HOOK ACTIVATED!");
            // System.out.println("[Exodar] ====================================");
            nametagHookDebugShown = true;
        }

        try {
            if (moduleManager != null) {
                // Check Nametags module first (primary)
                io.github.exodar.module.Module nametagsModule = moduleManager.getModuleByName("Nametags");
                if (nametagsModule != null && nametagsModule instanceof Nametags) {
                    if (((Nametags) nametagsModule).shouldHideVanillaNametags()) {
                        canceler.cancel = true;
                        return;
                    }
                }

                // Also check NoVanillaNametags module (legacy/backup)
                io.github.exodar.module.Module noNametagsModule = moduleManager.getModuleByName("NoVanillaNametags");
                if (noNametagsModule != null && noNametagsModule.isEnabled()) {
                    canceler.cancel = true;
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    // ===============================================
    // REACH / PENETRATION - GET MOUSE OVER HOOK
    // ===============================================

    private static boolean reachHookDebugShown = false;
    private static io.github.exodar.module.modules.combat.Reach reachModuleCache = null;
    private static io.github.exodar.module.modules.combat.Penetration2 penetrationModuleCache = null;

    /**
     * Hook into EntityRenderer.getMouseOver to extend reach and penetration
     * Method signature: getMouseOver(float partialTicks)
     *
     * NOTE: DISABLED for Optifine shader compatibility.
     * Bytecode patching EntityRenderer with COMPUTE_FRAMES breaks shaders.
     * Reach/Penetration will be handled via EntityRenderHook proxy instead.
     */
    // @EventHandler(type=ON_RETURN_THROW,
    //         targetClass = "net/minecraft/client/renderer/EntityRenderer",
    //         targetMethodName = "getMouseOver",
    //         targetMethodDescriptor = "(F)V",
    //         targetMethodIsStatic = false)
    public static void onGetMouseOver(Thrower thrower, EntityRenderer renderer, float partialTicks) {
        if (!reachHookDebugShown) {
            // System.out.println("[Exodar] ====================================");
            // System.out.println("[Exodar] REACH/PENETRATION HOOK (getMouseOver) ACTIVATED!");
            // System.out.println("[Exodar] ====================================");
            reachHookDebugShown = true;
        }

        try {
            if (moduleManager == null) return;

            // Cache modules
            if (reachModuleCache == null) {
                io.github.exodar.module.Module m = moduleManager.getModuleByName("Reach");
                if (m instanceof io.github.exodar.module.modules.combat.Reach) {
                    reachModuleCache = (io.github.exodar.module.modules.combat.Reach) m;
                }
            }
            if (penetrationModuleCache == null) {
                io.github.exodar.module.Module m = moduleManager.getModuleByName("Penetration");
                if (m instanceof io.github.exodar.module.modules.combat.Penetration2) {
                    penetrationModuleCache = (io.github.exodar.module.modules.combat.Penetration2) m;
                }
            }

            // Penetration first (3.0 range through blocks, or lets Reach handle if Reach is on)
            if (penetrationModuleCache != null && penetrationModuleCache.isEnabled()) {
                penetrationModuleCache.extendReachThroughBlocks();
            }

            // Then Reach (extended range, respects Penetration for through-blocks)
            if (reachModuleCache != null && reachModuleCache.isEnabled()) {
                reachModuleCache.extendReach();
            }

            // LegitAura3 - Override raycast to target aim target
            if (io.github.exodar.module.modules.combat.LegitAura3.isActive()) {
                io.github.exodar.module.modules.combat.LegitAura3 la3 = io.github.exodar.module.modules.combat.LegitAura3.getInstance();
                if (la3 != null) {
                    la3.overrideRaycast();
                }
            }
        } catch (Exception e) {
            // if (reachHookDebugShown) {
            //     // System.out.println("[Exodar] Error in onGetMouseOver: " + e.getMessage());
            // }
        }
    }

    /**
     * Called from EntityRenderHook.EntityRendererProxy.getMouseOver()
     * This is the proxy-based hook for Reach and Penetration modules.
     * Unlike the bytecode-based onGetMouseOver, this doesn't need a Thrower parameter.
     */
    public static void onGetMouseOverProxy(EntityRenderer renderer, float partialTicks) {
        if (!reachHookDebugShown) {
            System.out.println("[Exodar] ====================================");
            System.out.println("[Exodar] REACH/PENETRATION PROXY HOOK ACTIVATED!");
            System.out.println("[Exodar] ====================================");
            reachHookDebugShown = true;
        }

        try {
            if (moduleManager == null) return;

            // Cache modules
            if (reachModuleCache == null) {
                io.github.exodar.module.Module m = moduleManager.getModuleByName("Reach");
                if (m instanceof io.github.exodar.module.modules.combat.Reach) {
                    reachModuleCache = (io.github.exodar.module.modules.combat.Reach) m;
                }
            }
            if (penetrationModuleCache == null) {
                io.github.exodar.module.Module m = moduleManager.getModuleByName("Penetration");
                if (m instanceof io.github.exodar.module.modules.combat.Penetration2) {
                    penetrationModuleCache = (io.github.exodar.module.modules.combat.Penetration2) m;
                }
            }

            // Penetration first (3.0 range through blocks, or lets Reach handle if Reach is on)
            if (penetrationModuleCache != null && penetrationModuleCache.isEnabled()) {
                penetrationModuleCache.extendReachThroughBlocks();
            }

            // Then Reach (extended range, respects Penetration for through-blocks)
            if (reachModuleCache != null && reachModuleCache.isEnabled()) {
                reachModuleCache.extendReach();
            }

            // LegitAura3 - Override raycast to target aim target
            if (io.github.exodar.module.modules.combat.LegitAura3.isActive()) {
                io.github.exodar.module.modules.combat.LegitAura3 la3 = io.github.exodar.module.modules.combat.LegitAura3.getInstance();
                if (la3 != null) {
                    la3.overrideRaycast();
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    // =====================================================
    // RENDER EVENTS VIA BYTECODE PATCHING
    // These hooks inject directly into EntityRenderer methods
    // This is BETTER than the proxy approach because:
    // 1. Doesn't replace mc.entityRenderer
    // 2. Works with Optifine shaders
    // 3. Works with Lunar Client motion blur
    // =====================================================

    private static boolean render2DHookShown = false;

    // NOTE: renderWorldPass bytecode hook was removed because ON_RETURN_THROW fires AFTER hand rendering,
    // causing ESP positions to be wrong. The friend's client injects BEFORE "hand" section.
    // For now, Render3DEvent is fired from EntityRenderHook proxy which fires BEFORE hand.
    // TODO: Implement custom patcher that injects before "hand" like reference client.

    /**
     * Hook into EntityRenderer.updateCameraAndRender to fire Render2DEvent
     * Method signature: updateCameraAndRender(float partialTicks, long nanoTime)
     * This fires at the END of the method, after all rendering is done.
     *
     * NOTE: DISABLED for Optifine shader compatibility.
     * Bytecode patching EntityRenderer with COMPUTE_FRAMES breaks shaders.
     * Render2DEvent is now fired from EntityRenderHook proxy instead.
     */
    // @EventHandler(type=ON_RETURN_THROW,
    //         targetClass = "net/minecraft/client/renderer/EntityRenderer",
    //         targetMethodName = "updateCameraAndRender",
    //         targetMethodDescriptor = "(FJ)V",
    //         targetMethodIsStatic = false)
    public static void onUpdateCameraAndRender(Thrower thrower, EntityRenderer renderer, float partialTicks, long nanoTime) {
        if (!render2DHookShown) {
            System.out.println("[Exodar] Render2D bytecode hook activated!");
            render2DHookShown = true;
        }

        try {
            io.github.exodar.event.EventBus.post(new io.github.exodar.event.Render2DEvent(partialTicks));
        } catch (Exception e) {
            // Silent fail
        }
    }
}
