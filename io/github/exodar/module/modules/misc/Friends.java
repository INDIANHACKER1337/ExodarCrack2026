/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ColorSetting;
import io.github.exodar.setting.TextSetting;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.friend.FriendManager;
import io.github.exodar.ui.ModuleNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Friends module - Middle click to add/remove friends
 * Friends are shown in cyan and not targeted by combat modules
 */
public class Friends extends Module {
    private ColorSetting friendColor;
    private TextSetting addFriendName;
    private TickSetting addFriendButton;
    private TickSetting clearAllButton;

    private boolean wasMiddlePressed = false;
    private static Field thePlayerField;
    private String lastAddedName = "";

    // Friend color constant (cyan)
    public static final int FRIEND_COLOR = 0x55FFFF;

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("EntityPlayer")) {
                    f.setAccessible(true);
                    thePlayerField = f;
                    break;
                }
            }
        } catch (Exception e) {
            // Silent
        }
    }

    public Friends() {
        super("Friends", ModuleCategory.MISC);
        this.registerSetting(new DescriptionSetting("Middle click or add manually"));
        this.registerSetting(friendColor = new ColorSetting("Friend Color", 85, 255, 255)); // Cyan
        this.registerSetting(new DescriptionSetting("--- Add Friend ---"));
        this.registerSetting(addFriendName = new TextSetting("Nick", "", 16));
        this.registerSetting(addFriendButton = new TickSetting("Add Friend", false));
        this.registerSetting(clearAllButton = new TickSetting("Clear All", false));
    }

    @Override
    public void onEnable() {
        wasMiddlePressed = false;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        // Check Add Friend button
        if (addFriendButton.isEnabled()) {
            String name = addFriendName.getValue().trim();
            if (!name.isEmpty() && !name.equals(lastAddedName)) {
                if (!FriendManager.getInstance().isFriend(name)) {
                    FriendManager.getInstance().addFriend(name);
                    ModuleNotification.addNotification("+ " + name, true);
                    lastAddedName = name;
                }
            }
            addFriendButton.setEnabled(false);
            addFriendName.setValue("");
            lastAddedName = "";
        }

        // Check Clear All button
        if (clearAllButton.isEnabled()) {
            int count = FriendManager.getInstance().getFriendCount();
            FriendManager.getInstance().clearFriends();
            ModuleNotification.addNotification("Cleared " + count + " friends", false);
            clearAllButton.setEnabled(false);
        }

        try {
            // Check middle mouse button (button 2)
            boolean middlePressed = Mouse.isButtonDown(2);

            // Detect middle click (press, not hold)
            if (middlePressed && !wasMiddlePressed) {
                // Only process when NO GUI is open (currentScreen must be null)
                // This prevents middle-click working in inventories, ClickGUI, etc.
                if (mc.currentScreen != null) {
                    wasMiddlePressed = middlePressed;
                    return; // GUI is open, don't process
                }

                EntityPlayer player = (EntityPlayer) thePlayerField.get(mc);
                if (player != null) {
                    // Get entity player is looking at
                    MovingObjectPosition mop = mc.objectMouseOver;
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        Entity entity = mop.entityHit;
                        if (entity instanceof EntityPlayer && entity != player) {
                            String targetName = entity.getName();
                            boolean added = FriendManager.getInstance().toggleFriend(targetName);

                            // Show notification
                            if (added) {
                                ModuleNotification.addNotification("+ " + targetName, true);
                            } else {
                                ModuleNotification.addNotification("- " + targetName, false);
                            }
                        }
                    }
                }
            }

            wasMiddlePressed = middlePressed;
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Check if a player is a friend
     */
    public static boolean isFriend(EntityPlayer player) {
        if (player == null) return false;
        return FriendManager.getInstance().isFriend(player.getName());
    }

    /**
     * Check if a player name is a friend
     */
    public static boolean isFriend(String name) {
        return FriendManager.getInstance().isFriend(name);
    }

    /**
     * Get the friend color
     */
    public int getFriendColor() {
        return friendColor.getColor();
    }

    /**
     * Get friend color statically (uses default cyan if module not available)
     */
    public static int getColor() {
        return FRIEND_COLOR;
    }

    @Override
    public String getDisplaySuffix() {
        int count = FriendManager.getInstance().getFriendCount();
        return " §7" + count;
    }
}
