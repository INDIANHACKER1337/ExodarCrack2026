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
import io.github.exodar.setting.TextSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.network.play.server.S02PacketChat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * KillSults - Send messages when you kill someone
 * Exodar Edition with multiple message categories
 */
public class KillSults extends Module {

    private final ModeSetting mode;
    private final TickSetting usePrefix;
    private final TextSetting prefix;
    private final SliderSetting delay;
    private final TickSetting customFile;

    private final Queue<String> chatQueue = new LinkedList<>();
    private long lastChatTime = 0;

    private final List<String> insults = new ArrayList<>();
    private final Random random = new Random();

    public KillSults() {
        super("KillSults", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("Send messages on kills"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"All", "Femboy", "Insult", "Meme"}));
        this.registerSetting(usePrefix = new TickSetting("Use Prefix", false));
        this.registerSetting(prefix = new TextSetting("Prefix", "/shout "));
        this.registerSetting(delay = new SliderSetting("Delay", 3000, 1000, 10000, 100));
        this.registerSetting(customFile = new TickSetting("Use Custom File", false));
    }

    @Override
    public void onEnable() {
        loadInsults();
        chatQueue.clear();
        lastChatTime = 0;
    }

    @Override
    public void onDisable() {
        chatQueue.clear();
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Reload insults if mode changed
        // Process chat queue with delay
        long now = System.currentTimeMillis();
        if (!chatQueue.isEmpty() && now - lastChatTime >= delay.getValue()) {
            String message = chatQueue.poll();
            if (message != null && !message.isEmpty()) {
                // Filter illegal characters before sending
                message = filterIllegalChars(message);
                if (!message.isEmpty()) {
                    mc.thePlayer.sendChatMessage(message);
                }
                lastChatTime = now;
            }
        }
    }

    /**
     * Filter out illegal characters that Minecraft chat doesn't allow
     */
    private String filterIllegalChars(String message) {
        StringBuilder sb = new StringBuilder();
        for (char c : message.toCharArray()) {
            // Minecraft allows: letters, numbers, spaces, and basic punctuation
            // ASCII 32-126 are generally safe, but some servers are stricter
            if (c >= 32 && c <= 126) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Clean player name - remove color codes and keep only valid username chars
     */
    private String cleanPlayerName(String name) {
        if (name == null || name.isEmpty()) return "";
        // Remove color codes (§X or remaining color code chars like 7, a, etc at start)
        String cleaned = name.replaceAll("\u00A7.", ""); // Remove §X
        // Only keep valid Minecraft username characters (letters, numbers, underscore)
        StringBuilder sb = new StringBuilder();
        for (char c : cleaned.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;
        if (mc.thePlayer == null || mc.theWorld == null) return true;

        if (packet instanceof S02PacketChat) {
            S02PacketChat chatPacket = (S02PacketChat) packet;
            String message = chatPacket.getChatComponent().getUnformattedText();

            // Detect kill messages (common formats)
            String playerName = mc.thePlayer.getName();
            if (message.contains("by " + playerName) && !message.contains("BED DESTRUCTION") && !message.contains("FINAL KILL")) {
                // Extract victim name (usually first word)
                String[] parts = message.split(" ");
                if (parts.length > 0) {
                    String victim = cleanPlayerName(parts[0]);
                    // Don't insult ourselves
                    if (!victim.equalsIgnoreCase(playerName) && !victim.isEmpty()) {
                        queueInsult(victim);
                    }
                }
            }
            // Also detect "killed by" format
            else if (message.contains(playerName + " killed") || message.contains(playerName + " shot") ||
                     message.contains(playerName + " slain")) {
                String[] parts = message.split(" ");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("killed") || parts[i].equals("shot") || parts[i].equals("slain")) {
                        if (i + 1 < parts.length) {
                            String victim = cleanPlayerName(parts[i + 1]);
                            if (!victim.equalsIgnoreCase(playerName) && !victim.isEmpty()) {
                                queueInsult(victim);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private void queueInsult(String victim) {
        // Reload insults each time to catch mode changes
        loadInsults();

        String insult = getRandomInsult(victim);
        if (usePrefix.isEnabled() && !prefix.getValue().isEmpty()) {
            insult = prefix.getValue() + insult;
        }
        chatQueue.add(insult);
    }

    private void loadInsults() {
        insults.clear();

        if (customFile.isEnabled()) {
            File file = getInsultsFile();
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            insults.add(line.trim());
                        }
                    }
                } catch (Exception e) {
                    // Silent fail
                }
            }
        }

        // Use defaults based on mode if no custom insults loaded
        if (insults.isEmpty()) {
            String currentMode = mode.getSelected();
            if (currentMode.equals("Femboy")) {
                insults.addAll(Arrays.asList(getFemboyInsults()));
            } else if (currentMode.equals("Insult")) {
                insults.addAll(Arrays.asList(getInsultMessages()));
            } else if (currentMode.equals("Meme")) {
                insults.addAll(Arrays.asList(getMemeMessages()));
            } else { // All
                insults.addAll(Arrays.asList(getFemboyInsults()));
                insults.addAll(Arrays.asList(getInsultMessages()));
                insults.addAll(Arrays.asList(getMemeMessages()));
            }
        }
    }

    private File getInsultsFile() {
        File dir = new File(System.getProperty("user.home"), ".exodar");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "killsults.txt");
    }

    private String getRandomInsult(String name) {
        if (insults.isEmpty()) return "GG " + name;
        String insult = insults.get(random.nextInt(insults.size()));
        return insult.replace("%s", name);
    }

    // ==================== FEMBOY/FURRY MESSAGES ====================
    private String[] getFemboyInsults() {
        return new String[]{
                "%s just got pawned by a femboy OwO",
                "UwU %s got destroyed",
                "%s, my thigh highs have more skill than you",
                "Nya %s got clapped harder than my tail wags",
                "%s just lost to someone in a maid outfit",
                "OwO whats this? %s is dead!",
                "%s got hit with the femboy combo",
                "notices your death OwO %s",
                "%s, even my cat ears couldnt save you",
                "Rawr XD %s got demolished",
                "%s just became my new scratching post UwU",
                "pounces on %s corpse OwO",
                "Nya %s got outplayed by superior catboy gaming",
                "%s thought they could beat a furry, how cute",
                "%s got the uwu treatment",
                "Sorry %s, femboy supremacy is real",
                "blushes oops I killed %s again UwU",
                "%s just lost to someone saying nya unironically",
                "Exodar sends its regards %s OwO",
                "%s, my programmer socks gave me +100 skill",
                "%s just got 621d nya",
                "Awoo %s is eliminated",
                "%s couldnt handle the fluff",
                "teleports behind %s nothing personal nya",
                "%s got bodied by furry engineering",
                "The pathOwOgen claims another: %s",
                "%s, my fursona sends regards",
                "Bark bark %s youre dead uwu",
                "%s got the full glomp combo",
                "Another victory for the fluffies %s",
                "%s got nuzzled to death OwO",
                "wiggles tail GG %s",
                "%s, you just got out-uwud",
                "Femboy gaming claims another victim: %s",
                "%s got destroyed by cat ear energy",
                "headpats %s grave OwO",
                "%s, the skirt gave me power",
                "Nyaa %s fell for the trap card",
                "%s got the maid cafe special",
                "boops %s nose ur ded uwu"
        };
    }

    // ==================== INSULT MESSAGES ====================
    private String[] getInsultMessages() {
        return new String[]{
                "Wow %s you just died in a block game",
                "%s, skill issue detected",
                "Thanks for the free kill %s!",
                "%s, my grandma plays better than you",
                "GG EZ %s",
                "%s got absolutely destroyed",
                "Hold this L %s",
                "RIP bozo %s",
                "%s, have you tried getting good?",
                "That was embarrassing %s",
                "%s just got outplayed hard",
                "Better luck next time %s... jk there won't be one",
                "%s, you're the type to lose in tutorial",
                "Imagine being %s right now",
                "%s got packed up",
                "Another victim: %s",
                "%s should consider retirement",
                "What was that %s?",
                "%s got absolutely bodied",
                "GG %s, now uninstall",
                "%s is down bad... literally",
                "Error: %s skill not found",
                "%s just got Exodar'd",
                "That's what you get %s",
                "%s, maybe try Fortnite instead?",
                "%s.exe has stopped working",
                "Task failed successfully: %s eliminated",
                "%s got calculated",
                "%s needs a better gaming chair",
                "Cope harder %s",
                // Vape/Slinky references
                "LMAO %s is a Vape user and still lost",
                "%s probably uses Vape Cracked lmfaooo",
                "%s, you should look into purchasing Vape... oh wait",
                "Imagine losing with Vape like %s",
                "%s got destroyed, must be using Slinky",
                "LMAO %s uses Slinky Cracked",
                "%s probably downloaded Slinky from a sketchy Discord",
                "%s, even Vape couldn't save you",
                "%s got clapped while using Slinky LMAO",
                "Not even Vape Lite could help %s",
                "%s, your Slinky config is trash",
                "%s probably asks for Vape Crack in Discord",
                "Imagine paying for Vape and still dying like %s",
                "%s uses cracked Slinky and still loses",
                "%s, Exodar > your Vape",
                "Did %s really just lose with Slinky enabled?",
                "%s probably thinks Vape Cracked is legit",
                "Slinky user %s got destroyed by Exodar",
                // Exodar/Exobobo references
                "Get good get Exodar %s",
                "%s just got destroyed by Exobobo",
                "Exobobo sends its regards %s",
                "%s, Exodar on top",
                "Another kill for Exobobo: %s",
                "%s got the Exodar special",
                "Exobobo better than %s",
                "%s meet Exodar, Exodar meet %s's corpse",
                "Exodar gaming claims %s",
                "%s fell to the power of Exobobo",
                "%s Vape subscription expired mid-fight"
        };
    }

    // ==================== MEME MESSAGES ====================
    private String[] getMemeMessages() {
        return new String[]{
                "%s got ratio'd in minecraft",
                "L + ratio + you fell off + %s is bad",
                "%s just got sent to the backrooms",
                "GG EZ %s, no cap fr fr",
                "%s caught in 4K losing",
                "That's a certified %s moment",
                "%s just got the Exodar experience",
                "POV: %s just got destroyed",
                "%s, this ain't it chief",
                "Not %s getting destroyed in 2024",
                "%s speedrunning to respawn screen",
                "%s is now grass (touch it)",
                "Exodar > %s confirmed",
                "Another one bites the dust: %s",
                "%s got the hands fr fr",
                "Easy clap %s, no printer",
                "%s just joined the shadow realm",
                "%s caught these hands bussin fr",
                "It's giving... %s losing",
                "%s just got cancelled",
                "Slay queen but %s is the one slain",
                "%s, that was lowkey mid",
                "No cap %s got destroyed",
                "Rent free in %s's head",
                "%s took the L, respectfully",
                "Vibe check failed: %s",
                "%s just got based'd and redpilled",
                "Living rent free killing %s",
                "%s got the Twitter treatment",
                "Skill diff %s, cry about it",
                "%s, that was not very poggers of you",
                "Cringe %s moment",
                "W Exodar, L %s",
                "%s got NPC'd",
                "Main character energy vs %s"
        };
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
