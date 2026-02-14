/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TextSetting;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.account.AccountManager;
import io.github.exodar.ui.ModuleNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * Account Switcher - Switch between cracked accounts
 */
public class AccountSwitcher extends Module {
    private TextSetting newAccountName;
    private TickSetting addAccountButton;

    private static java.util.List<Field> sessionFields = new java.util.ArrayList<>();

    static {
        // Find ALL session fields in Minecraft class (Lunar may have multiple)
        try {
            Minecraft mc = Minecraft.getMinecraft();

            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType() == Session.class) {
                    f.setAccessible(true);
                    sessionFields.add(f);
                    System.out.println("[AccountSwitcher] Found session field: " + f.getName());
                }
            }

            // Also check superclasses
            Class<?> superClass = mc.getClass().getSuperclass();
            while (superClass != null && superClass != Object.class) {
                for (Field f : superClass.getDeclaredFields()) {
                    if (f.getType() == Session.class) {
                        f.setAccessible(true);
                        sessionFields.add(f);
                        System.out.println("[AccountSwitcher] Found session field in superclass: " + f.getName());
                    }
                }
                superClass = superClass.getSuperclass();
            }

            System.out.println("[AccountSwitcher] Total session fields: " + sessionFields.size());
        } catch (Exception e) {
            System.out.println("[AccountSwitcher] Error finding session fields: " + e.getMessage());
        }
    }

    public AccountSwitcher() {
        super("Accounts", ModuleCategory.MISC);
        this.registerSetting(new DescriptionSetting("Cracked account switcher"));
        this.registerSetting(new DescriptionSetting("--- Add Account ---"));
        this.registerSetting(newAccountName = new TextSetting("Username", "", 16));
        this.registerSetting(addAccountButton = new TickSetting("Add Account", false));

        // Add existing accounts as description settings
        refreshAccountList();
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        // Check Add Account button
        if (addAccountButton.isEnabled()) {
            String username = newAccountName.getValue().trim();
            if (!username.isEmpty()) {
                AccountManager.getInstance().addAccount(username);
                ModuleNotification.addNotification("Added: " + username, true);
                newAccountName.setValue("");
                refreshAccountList();
            }
            addAccountButton.setEnabled(false);
        }
    }

    /**
     * Switch to a cracked account
     */
    public static boolean switchAccount(String username) {
        if (username == null || username.isEmpty()) return false;

        try {
            Minecraft mc = Minecraft.getMinecraft();
            String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()).toString().replace("-", "");

            // Create offline session
            Session newSession = new Session(
                username,
                uuid,
                "0",
                "legacy"
            );

            if (sessionFields.isEmpty()) {
                System.out.println("[AccountSwitcher] ERROR: No session fields found!");
                return false;
            }

            // Update ALL session fields
            int fieldsUpdated = 0;
            for (Field field : sessionFields) {
                try {
                    field.set(mc, newSession);
                    fieldsUpdated++;
                } catch (Exception e) {
                    System.out.println("[AccountSwitcher] Failed to update " + field.getName());
                }
            }

            AccountManager.getInstance().setCurrentAccount(username);
            System.out.println("[AccountSwitcher] Switched to: " + username + " (updated " + fieldsUpdated + " fields)");
            return fieldsUpdated > 0;
        } catch (Exception e) {
            System.out.println("[AccountSwitcher] Error switching account: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get current session username
     */
    public static String getCurrentUsername() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            Session session = mc.getSession();
            return session != null ? session.getUsername() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void refreshAccountList() {
        // This would ideally refresh the settings list, but for now accounts are managed via AccountManager
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + getCurrentUsername();
    }
}
