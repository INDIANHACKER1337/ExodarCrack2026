/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.account;

import java.io.*;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import io.github.exodar.account.auth.MicrosoftAccount;
import io.github.exodar.account.auth.MicrosoftLogin;

public class AccountManager {
    private static AccountManager instance;
    private final List<AccountData> crackedAccounts = new ArrayList<>();
    private final List<MicrosoftAccount> microsoftAccounts = new ArrayList<>();

    private static final String CRACKED_FILE = "exodar_accounts.txt";
    private static final String MICROSOFT_FILE = "exodar_microsoft.json";

    private String currentAccount = null;
    private boolean currentIsMicrosoft = false;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final String[] ADJECTIVES = {
        "Swift", "Dark", "Fire", "Ice", "Storm", "Shadow", "Bright", "Wild",
        "Sharp", "Bold", "Cool", "Epic", "Fast", "Gold", "Iron", "Mega",
        "Neo", "Pro", "Red", "Blue", "Green", "Black", "White", "Gray",
        "Toxic", "Ultra", "Void", "Warp", "Zero", "Alpha", "Beta", "Delta",
        "Omega", "Prime", "Nova", "Flux", "Apex", "Blaze", "Frost", "Thunder"
    };

    private static final String[] NOUNS = {
        "Wolf", "Fox", "Bear", "Hawk", "Lion", "Tiger", "Dragon", "Phoenix",
        "Knight", "Ninja", "Mage", "King", "Lord", "Boss", "Ace", "Star",
        "Blade", "Storm", "Fire", "Ice", "Wind", "Rock", "Ghost", "Spirit",
        "Titan", "Giant", "Hero", "Rider", "Hunter", "Slayer", "Master", "Legend",
        "Viper", "Cobra", "Shark", "Eagle", "Raven", "Panther", "Demon", "Angel"
    };

    private static final Random random = new Random();

    private AccountManager() {
        load();
    }

    public static AccountManager getInstance() {
        if (instance == null) {
            instance = new AccountManager();
        }
        return instance;
    }

    public static class AccountData {
        public String username;
        public boolean favorite;
        public long addedTime;

        public AccountData(String username) {
            this.username = username;
            this.favorite = false;
            this.addedTime = System.currentTimeMillis();
        }

        public AccountData(String username, boolean favorite) {
            this.username = username;
            this.favorite = favorite;
            this.addedTime = System.currentTimeMillis();
        }
    }

    // ==================== MICROSOFT ACCOUNTS ====================

    public void addMicrosoftAccount(MicrosoftAccount account) {
        if (account == null || !account.isValid()) return;

        for (int i = 0; i < microsoftAccounts.size(); i++) {
            if (microsoftAccounts.get(i).uuid != null && microsoftAccounts.get(i).uuid.equals(account.uuid)) {
                microsoftAccounts.set(i, account);
                saveMicrosoftAccounts();
                return;
            }
        }

        microsoftAccounts.add(account);
        saveMicrosoftAccounts();
        System.out.println("[AccountManager] Added Microsoft account: " + account.username);
    }

    public void removeMicrosoftAccount(String uuid) {
        microsoftAccounts.removeIf(acc -> acc.uuid != null && acc.uuid.equals(uuid));
        saveMicrosoftAccounts();
    }

    public void removeMicrosoftAccountByName(String username) {
        microsoftAccounts.removeIf(acc -> acc.username != null && acc.username.equalsIgnoreCase(username));
        saveMicrosoftAccounts();
    }

    public List<MicrosoftAccount> getMicrosoftAccounts() {
        List<MicrosoftAccount> sorted = new ArrayList<>(microsoftAccounts);
        sorted.sort((a, b) -> {
            if (a.favorite && !b.favorite) return -1;
            if (!a.favorite && b.favorite) return 1;
            return Long.compare(b.lastLogin, a.lastLogin);
        });
        return sorted;
    }

    public MicrosoftAccount getMicrosoftAccount(String uuid) {
        for (MicrosoftAccount acc : microsoftAccounts) {
            if (acc.uuid != null && acc.uuid.equals(uuid)) {
                return acc;
            }
        }
        return null;
    }

    public MicrosoftAccount getMicrosoftAccountByName(String username) {
        for (MicrosoftAccount acc : microsoftAccounts) {
            if (acc.username != null && acc.username.equalsIgnoreCase(username)) {
                return acc;
            }
        }
        return null;
    }

    public void toggleMicrosoftFavorite(String uuid) {
        for (MicrosoftAccount acc : microsoftAccounts) {
            if (acc.uuid != null && acc.uuid.equals(uuid)) {
                acc.favorite = !acc.favorite;
                saveMicrosoftAccounts();
                return;
            }
        }
    }

    public boolean loginMicrosoftAccount(MicrosoftAccount account) {
        if (account == null || account.refreshToken == null) return false;

        MicrosoftAccount refreshed = MicrosoftLogin.loginWithRefreshToken(account.refreshToken);
        if (refreshed != null) {
            account.mcToken = refreshed.mcToken;
            account.refreshToken = refreshed.refreshToken;
            account.lastLogin = System.currentTimeMillis();
            saveMicrosoftAccounts();

            currentAccount = account.username;
            currentIsMicrosoft = true;
            return true;
        }
        return false;
    }

    public void startMicrosoftLogin() {
        MicrosoftLogin.startLogin(account -> {
            if (account != null) {
                addMicrosoftAccount(account);
                currentAccount = account.username;
                currentIsMicrosoft = true;
            }
        });
    }

    public int getMicrosoftAccountCount() {
        return microsoftAccounts.size();
    }

    // ==================== CRACKED ACCOUNTS ====================

    public void addAccount(String username) {
        if (username != null && !username.isEmpty() && !hasAccount(username)) {
            crackedAccounts.add(new AccountData(username));
            saveCrackedAccounts();
        }
    }

    public void addAccount(String username, boolean favorite) {
        if (username != null && !username.isEmpty() && !hasAccount(username)) {
            crackedAccounts.add(new AccountData(username, favorite));
            saveCrackedAccounts();
        }
    }

    public boolean hasAccount(String username) {
        for (AccountData acc : crackedAccounts) {
            if (acc.username.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    public void removeAccount(String username) {
        crackedAccounts.removeIf(acc -> acc.username.equalsIgnoreCase(username));
        saveCrackedAccounts();
    }

    public void toggleFavorite(String username) {
        for (AccountData acc : crackedAccounts) {
            if (acc.username.equalsIgnoreCase(username)) {
                acc.favorite = !acc.favorite;
                saveCrackedAccounts();
                return;
            }
        }
    }

    public boolean isFavorite(String username) {
        for (AccountData acc : crackedAccounts) {
            if (acc.username.equalsIgnoreCase(username)) {
                return acc.favorite;
            }
        }
        return false;
    }

    public List<String> getAccounts() {
        List<AccountData> sorted = new ArrayList<>(crackedAccounts);
        sorted.sort((a, b) -> {
            if (a.favorite && !b.favorite) return -1;
            if (!a.favorite && b.favorite) return 1;
            return Long.compare(b.addedTime, a.addedTime);
        });

        List<String> result = new ArrayList<>();
        for (AccountData acc : sorted) {
            result.add(acc.username);
        }
        return result;
    }

    public List<AccountData> getAccountData() {
        return new ArrayList<>(crackedAccounts);
    }

    public int getAccountCount() {
        return crackedAccounts.size();
    }

    public int getFavoriteCount() {
        int count = 0;
        for (AccountData acc : crackedAccounts) {
            if (acc.favorite) count++;
        }
        return count;
    }

    public String getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(String account) {
        this.currentAccount = account;
    }

    public boolean isCurrentMicrosoft() {
        return currentIsMicrosoft;
    }

    public void setCurrentIsMicrosoft(boolean isMicrosoft) {
        this.currentIsMicrosoft = isMicrosoft;
    }

    public static String generateRandomUsername() {
        String adj = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[random.nextInt(NOUNS.length)];

        int nameLength = adj.length() + noun.length();
        int maxNumberLength = 16 - nameLength;

        String number = "";
        if (maxNumberLength > 0) {
            int maxNum = (int) Math.pow(10, Math.min(maxNumberLength, 4)) - 1;
            int num = random.nextInt(maxNum) + 1;
            number = String.valueOf(num);
        }

        String result = adj + noun + number;

        if (result.length() > 16) {
            result = result.substring(0, 16);
        }

        return result;
    }

    // ==================== SAVE/LOAD ====================

    private File getMinecraftDir() {
        File mcDir = new File(System.getProperty("user.home"), ".minecraft");
        if (!mcDir.exists()) {
            mcDir = new File(".");
        }
        return mcDir;
    }

    public void save() {
        saveCrackedAccounts();
        saveMicrosoftAccounts();
    }

    private void saveCrackedAccounts() {
        try {
            File accountsFile = new File(getMinecraftDir(), CRACKED_FILE);
            try (PrintWriter writer = new PrintWriter(new FileWriter(accountsFile))) {
                for (AccountData acc : crackedAccounts) {
                    writer.println(acc.username + ":" + acc.favorite + ":" + acc.addedTime);
                }
            }
        } catch (Exception e) {
            System.out.println("[AccountManager] Error saving cracked accounts: " + e.getMessage());
        }
    }

    private void saveMicrosoftAccounts() {
        try {
            File msFile = new File(getMinecraftDir(), MICROSOFT_FILE);
            try (Writer writer = new FileWriter(msFile)) {
                gson.toJson(microsoftAccounts, writer);
            }
            System.out.println("[AccountManager] Saved " + microsoftAccounts.size() + " Microsoft accounts");
        } catch (Exception e) {
            System.out.println("[AccountManager] Error saving Microsoft accounts: " + e.getMessage());
        }
    }

    public void load() {
        loadCrackedAccounts();
        loadMicrosoftAccounts();
    }

    private void loadCrackedAccounts() {
        try {
            File accountsFile = new File(getMinecraftDir(), CRACKED_FILE);
            if (!accountsFile.exists()) return;

            crackedAccounts.clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(accountsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        String[] parts = line.split(":");
                        if (parts.length >= 3) {
                            AccountData acc = new AccountData(parts[0]);
                            acc.favorite = Boolean.parseBoolean(parts[1]);
                            try {
                                acc.addedTime = Long.parseLong(parts[2]);
                            } catch (NumberFormatException e) {
                                acc.addedTime = System.currentTimeMillis();
                            }
                            crackedAccounts.add(acc);
                        } else if (parts.length == 2) {
                            AccountData acc = new AccountData(parts[0]);
                            acc.favorite = Boolean.parseBoolean(parts[1]);
                            crackedAccounts.add(acc);
                        } else {
                            crackedAccounts.add(new AccountData(line));
                        }
                    }
                }
            }
            System.out.println("[AccountManager] Loaded " + crackedAccounts.size() + " cracked accounts");
        } catch (Exception e) {
            System.out.println("[AccountManager] Error loading cracked accounts: " + e.getMessage());
        }
    }

    private void loadMicrosoftAccounts() {
        try {
            File msFile = new File(getMinecraftDir(), MICROSOFT_FILE);
            if (!msFile.exists()) return;

            microsoftAccounts.clear();
            try (Reader reader = new FileReader(msFile)) {
                List<MicrosoftAccount> loaded = gson.fromJson(reader, new TypeToken<List<MicrosoftAccount>>(){}.getType());
                if (loaded != null) {
                    microsoftAccounts.addAll(loaded);
                }
            }
            System.out.println("[AccountManager] Loaded " + microsoftAccounts.size() + " Microsoft accounts");
        } catch (Exception e) {
            System.out.println("[AccountManager] Error loading Microsoft accounts: " + e.getMessage());
        }
    }
}
